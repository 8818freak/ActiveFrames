package de.herbers.activeframes;

import android.accessibilityservice.AccessibilityService;
import android.graphics.Bitmap;
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

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return;
        CharSequence p = event.getPackageName();
        if (p == null) return;
        String pkg = p.toString();
        if (pkg.equals(getPackageName())) return;
        try {
            if (getPackageManager().getLaunchIntentForPackage(pkg) == null) return; // nur startbare Apps
        } catch (Throwable t) { return; }

        if (pkg.equals(lastForeground)) return; // gleiche App (nur Dialog o.ae.) -> nichts tun
        lastForeground = pkg;
        currentPkg = pkg;
        // Die App wurde gerade benutzt -> nicht mehr als "verworfen" behandeln
        // und die MRU-Reihenfolge des Widgets sofort auffrischen (sonst blieb
        // die Liste stehen und die zuletzt benutzte App nicht oben links).
        ImageStore.clearDismissed(this, pkg);
        FramesWidget.refreshData(this);
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
                                        ImageStore.putImage(FramesAccessibilityService.this, pkg, soft);
                                        FramesWidget.refreshData(FramesAccessibilityService.this);
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

    @Override public void onInterrupt() {}
}
