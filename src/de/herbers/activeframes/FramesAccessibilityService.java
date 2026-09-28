package de.herbers.activeframes;

import android.accessibilityservice.AccessibilityService;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.hardware.HardwareBuffer;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.Display;
import android.view.accessibility.AccessibilityEvent;

/** Optionaler Bedienungshilfe-Dienst - der einzige Weg fuer eine Nicht-System-
 *  App, ECHTE Bilder des zuletzt angezeigten App-Zustands auf die Kacheln zu
 *  bringen (wie BlackBerrys Active Frames), ohne Root und ohne das
 *  MediaProjection-Warnsymbol: {@link AccessibilityService#takeScreenshot}
 *  (ab Android 11). Wird nur aktiv, wenn der Nutzer die Bedienungshilfe
 *  ausdruecklich einschaltet. Aufnahmen bleiben lokal (app-interner Cache),
 *  nichts wird gesendet; als sicher markierte Fenster (Banking o.ae.) kommen
 *  ohnehin schwarz. */
public class FramesAccessibilityService extends AccessibilityService {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private volatile String currentPkg;
    private String lastForeground;
    private Runnable pending;
    private Runnable pendingReorder;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return;
        CharSequence p = event.getPackageName();
        if (p == null) return;
        String pkg = p.toString();
        if (pkg.equals(getPackageName())) return;
        // Den Start-Launcher NICHT als geoeffnete App behandeln: sonst wuerde
        // beim Zurueckkehren zum Startbildschirm der Launcher selbst als "zuletzt
        // geoeffnet" vorgemerkt und bekaeme (mit einem Foto des Startbildschirms)
        // eine Kachel oben - genau das verschob die Reihenfolge (UsageProvider
        // filtert den Launcher ohnehin heraus, die lastOpened-Vormerkung tat es
        // bisher nicht).
        String launcher = FramesWidget.launcherPkg(this);
        if (launcher != null && pkg.equals(launcher)) return;
        try {
            if (getPackageManager().getLaunchIntentForPackage(pkg) == null) return; // nur startbare Apps
        } catch (Throwable t) { return; }

        if (pkg.equals(lastForeground)) return; // gleiche App (nur Dialog o.ae.) -> nichts tun
        lastForeground = pkg;
        currentPkg = pkg;
        // Die App wurde gerade benutzt -> nicht mehr als "verworfen" behandeln
        // und die MRU-Reihenfolge des Widgets sofort auffrischen (sonst blieb
        // die Liste stehen und die zuletzt benutzte App nicht oben links).
        // App als "gerade geöffnet" merken, damit sie beim nächsten (vollen)
        // Neuaufbau sofort oben links steht, auch bevor der UsageStatsManager
        // nachgezogen hat.
        Settings.setLastOpened(this, pkg);
        ImageStore.clearDismissed(this, pkg);
        // Sofort schonend auffrischen (Stern/Verworfen billig aktualisieren).
        // Ein voller Neuaufbau (setRemoteAdapter) darf hier NICHT bei jedem
        // App-Wechsel-Ereignis feuern: das ueberfordert den BlackBerry Launcher,
        // die Scroll-Sammlung wird kalt, Kacheln teils nicht mehr anklickbar
        // (der 0.21-Rueckschritt).
        FramesWidget.refreshData(this);
        // Im Hybrid-Modus zusaetzlich EINEN gebuendelten, kurz verzoegerten
        // vollen Neuaufbau planen (hoechstens einmal pro echtem App-Wechsel,
        // nicht pro Fenster-Ereignis): nur so uebernimmt der BlackBerry Launcher
        // die neue Reihenfolge. Die Verzoegerung gibt dem UsageStatsManager Zeit
        // nachzuziehen (zusammen mit Settings.lastOpened -> korrekte Ordnung);
        // unsichtbar, weil die geoeffnete App den Startbildschirm verdeckt.
        if (Settings.scroll(this) && Settings.gentleRefresh(this) && Settings.gentleFullClose(this)) {
            if (pendingReorder != null) handler.removeCallbacks(pendingReorder);
            pendingReorder = () -> FramesWidget.rebuildAll(this);
            handler.postDelayed(pendingReorder, 900);
        }
        if (pending != null) handler.removeCallbacks(pending);
        // Kurz warten, bis die App wirklich gezeichnet hat, dann abfotografieren.
        pending = () -> capture(pkg);
        handler.postDelayed(pending, 650);
    }

    private void capture(String pkg) {
        if (!pkg.equals(currentPkg)) return;         // schon wieder gewechselt
        if (Build.VERSION.SDK_INT < 30) return;       // takeScreenshot erst ab Android 11
        try {
            takeScreenshot(Display.DEFAULT_DISPLAY, getMainExecutor(),
                    new TakeScreenshotCallback() {
                        @Override public void onSuccess(ScreenshotResult result) {
                            HardwareBuffer buffer = null;
                            try {
                                buffer = result.getHardwareBuffer();
                                Bitmap hw = Bitmap.wrapHardwareBuffer(buffer, result.getColorSpace());
                                if (hw != null) {
                                    Bitmap soft = hw.copy(Bitmap.Config.ARGB_8888, false);
                                    hw.recycle();
                                    if (soft != null) {
                                        // Apps, die Bildschirmfotos unterbinden (z.B. BBMe,
                                        // Banking), liefern ein schwarzes Bild. Dann statt
                                        // des Schwarzbilds das App-Logo kachelgross (mittig,
                                        // unverzerrt, unbeschnitten) zeigen.
                                        Bitmap store = isMostlyBlack(soft) ? logoTile(pkg) : soft;
                                        if (store != null) {
                                            ImageStore.putImage(FramesAccessibilityService.this, pkg, store);
                                            FramesWidget.refreshData(FramesAccessibilityService.this);
                                        }
                                    }
                                }
                            } catch (Throwable ignored) {
                            } finally {
                                if (buffer != null) try { buffer.close(); } catch (Throwable ignored) {}
                            }
                        }
                        @Override public void onFailure(int errorCode) {
                            // z.B. Taktgrenze (max ~1 Foto/Sekunde) - einfach ueberspringen.
                        }
                    });
        } catch (Throwable ignored) {}
    }

    /** Grob pruefen, ob das Bild praktisch komplett schwarz ist - so liefern
     *  Apps, die Bildschirmfotos unterbinden (FLAG_SECURE), ihr "Foto". Es wird
     *  ein Raster von Bildpunkten abgetastet; sind fast alle sehr dunkel, gilt
     *  das Bild als schwarz. */
    private boolean isMostlyBlack(Bitmap b) {
        try {
            int w = b.getWidth(), h = b.getHeight();
            if (w <= 0 || h <= 0) return true;
            int stepX = Math.max(1, w / 24), stepY = Math.max(1, h / 40);
            int total = 0, dark = 0;
            for (int y = 0; y < h; y += stepY) {
                for (int x = 0; x < w; x += stepX) {
                    int c = b.getPixel(x, y);
                    total++;
                    if (Color.red(c) < 14 && Color.green(c) < 14 && Color.blue(c) < 14) dark++;
                }
            }
            return total > 0 && dark >= total * 0.99;
        } catch (Throwable t) {
            return false;
        }
    }

    /** Kachel mit dem App-Logo mittig auf dunklem Grund (gemeinsamer Helfer) -
     *  fuer Apps, die Bildschirmfotos unterbinden (statt Schwarzbild). */
    private Bitmap logoTile(String pkg) {
        return AppInfoCache.logoTile(this, pkg, 320, 420);
    }

    @Override public void onInterrupt() {}
}
