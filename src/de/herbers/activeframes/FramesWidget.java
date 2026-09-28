package de.herbers.activeframes;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Kachel-Widget. Baut die komplette Ansicht als EINE RemoteViews auf (Zeilen
 *  und Kacheln per addView), NICHT über einen RemoteViews-Sammlungs-Adapter.
 *  Grund: der (alte) Launcher-Host recycelte Sammlungs-Kacheln fehlerhaft und
 *  wendete beim Wiederverwenden nur einen Teil der Änderungen an -> Bild/Name
 *  gehörten zu verschiedenen Apps. Ohne Adapter gibt es kein Recycling, jede
 *  Kachel ist eine eigene, vollständig gesetzte RemoteViews. Das erlaubt
 *  außerdem variable Reihenhöhen (obere Reihen groß, danach kleiner). Preis:
 *  kein Scrollen - sichtbar ist, was in die Widget-Größe passt. */
public class FramesWidget extends AppWidgetProvider {

    static final String ACTION_LAUNCH = "de.herbers.activeframes.LAUNCH";
    static final String EXTRA_PKG = "pkg";
    static final String EXTRA_DISMISS = "dismiss";

    @Override
    public void onUpdate(Context ctx, AppWidgetManager mgr, int[] ids) {
        for (int id : ids) updateWidget(ctx, mgr, id);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context ctx, AppWidgetManager mgr, int id, Bundle newOptions) {
        updateWidget(ctx, mgr, id);
    }

    @Override
    public void onReceive(Context ctx, Intent intent) {
        super.onReceive(ctx, intent);
        String action = intent.getAction();
        if (ACTION_LAUNCH.equals(action)) {
            String pkg = intent.getStringExtra(EXTRA_PKG);
            if (pkg == null) return;
            if (intent.getBooleanExtra(EXTRA_DISMISS, false)) {
                ImageStore.markDismissed(ctx, pkg);
                refreshAfterAction(ctx);
                return;
            }
            ImageStore.clearUnread(ctx, pkg);
            ImageStore.clearDismissed(ctx, pkg);
            Settings.setLastOpened(ctx, pkg);
            try {
                Intent launch = ctx.getPackageManager().getLaunchIntentForPackage(pkg);
                if (launch != null) {
                    launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    ctx.startActivity(launch);
                }
            } catch (Throwable ignored) {}
            refreshAfterAction(ctx);
        } else if (Intent.ACTION_USER_PRESENT.equals(action)) {
            refreshData(ctx);
        }
    }

    /** Alle Widget-Instanzen auffrischen.
     *
     *  Standard ist der vollständige updateWidget-Weg (auch im Scroll-Modus):
     *  Der BlackBerry Launcher reagiert nur darauf; das schonendere
     *  notifyAppWidgetViewDataChanged aktualisiert die Sammlung dort NICHT
     *  (dann ließe sich z.B. keine Kachel per ✕ schließen). Preis im Scroll-
     *  Modus: der Launcher baut die Sammlung neu auf (kurzes Bild-"Springen").
     *
     *  Ist „Schonend auffrischen" eingeschaltet (nur sinnvoll auf MODERNEN
     *  Launchern, die es unterstützen), wird im Scroll-Modus nur
     *  notifyAppWidgetViewDataChanged gemeldet - dort ohne Springen. */
    static void refreshData(Context ctx) {
        AppWidgetManager mgr = AppWidgetManager.getInstance(ctx);
        int[] ids = mgr.getAppWidgetIds(new ComponentName(ctx, FramesWidget.class));
        if (ids == null) return;
        boolean gentle = Settings.scroll(ctx) && Settings.gentleRefresh(ctx);
        for (int id : ids) {
            if (gentle) mgr.notifyAppWidgetViewDataChanged(id, R.id.grid);
            else updateWidget(ctx, mgr, id);
        }
    }

    /** Auffrischen nach einer NUTZERAKTION, die die Reihenfolge oder den
     *  Kachelsatz ändert: eine Kachel (oder eine App aus dem Launcher) öffnen
     *  bzw. eine Kachel schließen.
     *
     *  Im Hybrid-Modus (Scrollen + „Schonend auffrischen" + „Bei Aktionen voll
     *  auffrischen") nimmt eine solche Aktion den VOLLEN, zuverlässigen Weg
     *  (updateAppWidget). Nur so übernimmt der BlackBerry Launcher die neue
     *  Reihenfolge und weckt die Sammlung wieder auf, sodass das schonende
     *  Auffrischen (Scrollen, eintreffende Benachrichtigungen, Bild-Nachschärfen)
     *  danach wieder greift. Beim Öffnen ist der volle Weg besonders günstig:
     *  die geöffnete App verdeckt den Startbildschirm, das kurze Neu-Aufbauen
     *  ist also unsichtbar. Sonst wie {@link #refreshData}. */
    static void refreshAfterAction(Context ctx) {
        if (Settings.scroll(ctx) && Settings.gentleRefresh(ctx)
                && Settings.gentleFullClose(ctx)) {
            rebuildAll(ctx);
        } else {
            refreshData(ctx);
        }
    }

    /** Vollständiger Neuaufbau (Adapter/Spalten/Modus) - nach
     *  Einstellungsänderungen nötig, unabhängig von der Auffrisch-Option. */
    static void rebuildAll(Context ctx) {
        AppWidgetManager mgr = AppWidgetManager.getInstance(ctx);
        int[] ids = mgr.getAppWidgetIds(new ComponentName(ctx, FramesWidget.class));
        if (ids == null) return;
        for (int id : ids) updateWidget(ctx, mgr, id);
    }

    static void updateWidget(Context ctx, AppWidgetManager mgr, int id) {
        RemoteViews root = new RemoteViews(ctx.getPackageName(), R.layout.widget);

        List<String> list = orderedPkgs(ctx);
        if (list.isEmpty()) {
            root.setViewVisibility(R.id.empty, View.VISIBLE);
            root.setViewVisibility(R.id.grid, View.GONE);
            root.setViewVisibility(R.id.container, View.GONE);
            mgr.updateAppWidget(id, root);
            return;
        }
        root.setViewVisibility(R.id.empty, View.GONE);

        // Scrollbare Variante (Sammlung) - mehr Kacheln als sichtbar, auf alten
        // Launchern aber fehlerhaftes Recycling (siehe Warnhinweis in den
        // Einstellungen). Sonst die feste, immer korrekte Raster-Variante.
        if (Settings.scroll(ctx)) {
            root.setViewVisibility(R.id.container, View.GONE);
            root.setViewVisibility(R.id.grid, View.VISIBLE);
            root.setInt(R.id.grid, "setNumColumns", Settings.columns(ctx));
            Intent svc = new Intent(ctx, FramesWidgetService.class);
            svc.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id);
            // EINDEUTIGE Adapter-Adresse je Aufbau erzwingt, dass der Launcher
            // die Sammlung KOMPLETT neu erzeugt, statt alte Item-Views zu
            // recyceln. Auf dem BlackBerry Launcher aktualisierte das Recycling
            // beim Umsortieren nur einen Teil (Klick-Ziel/Reihenfolge stimmten,
            // aber Bild/Symbol/Name der Kachel blieben veraltet stehen -> Kachel
            // zeigte die falsche App, obwohl der Tipp die richtige öffnete). Mit
            // einer je Aufbau anderen Data-URI sieht der Launcher einen neuen
            // Adapter und zeichnet jede Kachel frisch. Der appWidgetId-Extra
            // (den die Factory liest) bleibt unverändert.
            svc.putExtra("af_rev", android.os.SystemClock.elapsedRealtime());
            svc.setData(Uri.parse(svc.toUri(Intent.URI_INTENT_SCHEME)));
            root.setRemoteAdapter(R.id.grid, svc);
            root.setEmptyView(R.id.grid, R.id.empty);
            Intent click = new Intent(ctx, FramesWidget.class).setAction(ACTION_LAUNCH);
            PendingIntent tmpl = PendingIntent.getBroadcast(ctx, 0, click,
                    PendingIntent.FLAG_MUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            root.setPendingIntentTemplate(R.id.grid, tmpl);
            mgr.updateAppWidget(id, root);
            mgr.notifyAppWidgetViewDataChanged(id, R.id.grid);
            return;
        }

        root.setViewVisibility(R.id.grid, View.GONE);
        root.setViewVisibility(R.id.container, View.VISIBLE);
        root.removeAllViews(R.id.container);

        int cols = Settings.columns(ctx);
        int fullH = tileHeightDp(ctx, mgr, id);
        int shortH = Math.max(48, Math.round(fullH * Settings.shortRowPercent(ctx) / 100f));
        int bigRows = Settings.bigRows(ctx);
        int imgEdge = imageEdge(list.size());

        int rows = (list.size() + cols - 1) / cols;
        for (int r = 0; r < rows; r++) {
            RemoteViews row = new RemoteViews(ctx.getPackageName(), R.layout.row);
            if (Build.VERSION.SDK_INT >= 31) {
                row.setViewLayoutHeight(R.id.row_lin, r < bigRows ? fullH : shortH,
                        TypedValue.COMPLEX_UNIT_DIP);
            }
            int added = 0;
            for (int c = 0; c < cols; c++) {
                int idx = r * cols + c;
                if (idx >= list.size()) break;
                row.addView(R.id.row_lin, buildTile(ctx, list.get(idx), imgEdge));
                added++;
            }
            for (int c = added; c < cols; c++) {
                row.addView(R.id.row_lin, new RemoteViews(ctx.getPackageName(), R.layout.filler));
            }
            root.addView(R.id.container, row);
        }
        mgr.updateAppWidget(id, root);
    }

    private static RemoteViews buildTile(Context ctx, String pkg, int maxEdge) {
        RemoteViews t = new RemoteViews(ctx.getPackageName(), R.layout.tile);
        String label = AppInfoCache.label(ctx, pkg);

        Bitmap small = AppInfoCache.icon(ctx, pkg, 40);
        if (small != null) t.setImageViewBitmap(R.id.tile_icon, small);
        t.setTextViewText(R.id.tile_label, label);

        // Kachelbild als kleines Bitmap DIREKT einbetten (nicht per content://-URI).
        // Sonst laedt der Launcher beim Neuaufbau des Widgets (z.B. nach dem
        // Schliessen einer Kachel) jede URI einzeln asynchron nach - die Bilder
        // "springen" dann kurz an die (durch das Wegfallen einer Kachel)
        // verschobenen Nachbarn, teils mehrfach hintereinander. Direkt
        // eingebettete Bitmaps sind sofort da -> kein Nachladen, kein Springen
        // (genau darum springt auch BlackBerrys Hub-Widget nicht). Die Groesse
        // ist an die Kachelzahl gekoppelt (imageEdge), damit die gesamte, in
        // EINER Transaktion uebertragene RemoteViews unter der ~1-MB-Binder-
        // Grenze bleibt. Kein Kachelbild -> das App-Logo kachelgross.
        Bitmap image = ImageStore.getImageScaledCached(ctx, pkg, maxEdge);
        if (image == null) {
            image = AppInfoCache.logoTileCached(ctx, pkg, Math.round(maxEdge * 0.75f), maxEdge);
        }
        if (image != null) {
            t.setImageViewBitmap(R.id.tile_image, image);
            t.setViewVisibility(R.id.tile_image, View.VISIBLE);
            t.setViewVisibility(R.id.tile_title, View.GONE);
        } else {
            t.setViewVisibility(R.id.tile_image, View.GONE);
            t.setTextViewText(R.id.tile_title, label);
            t.setViewVisibility(R.id.tile_title, View.VISIBLE);
        }
        t.setViewVisibility(R.id.tile_star, ImageStore.isUnread(ctx, pkg) ? View.VISIBLE : View.GONE);

        t.setOnClickPendingIntent(R.id.tile_card, pi(ctx, pkg, false));
        t.setOnClickPendingIntent(R.id.tile_close, pi(ctx, pkg, true));
        return t;
    }

    /** Bild-Kantenlaenge (px) je Kachel, an die Kachelzahl gekoppelt: bei mehr
     *  Kacheln kleinere Bilder, damit die gesamte, direkt eingebettete
     *  RemoteViews deutlich unter der ~1-MB-Binder-Grenze bleibt. */
    private static int imageEdge(int n) {
        int e = (int) Math.sqrt(200000.0 / Math.max(1, n));
        return Math.max(48, Math.min(176, e));
    }

    private static PendingIntent pi(Context ctx, String pkg, boolean dismiss) {
        Intent i = new Intent(ctx, FramesWidget.class)
                .setAction(ACTION_LAUNCH)
                .setData(Uri.parse((dismiss ? "af://dismiss/" : "af://open/") + pkg))
                .putExtra(EXTRA_PKG, pkg);
        if (dismiss) i.putExtra(EXTRA_DISMISS, true);
        int rc = ((dismiss ? "d" : "o") + pkg).hashCode();
        return PendingIntent.getBroadcast(ctx, rc, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    /** Angepinnte Schnellstart-Apps zuerst, dann MRU, ohne verworfene, gekappt. */
    static List<String> orderedPkgs(Context ctx) {
        int max = Settings.maxTiles(ctx);
        Set<String> dismissed = ImageStore.dismissed(ctx);
        List<String> out = new ArrayList<>();
        for (String p : Settings.pinned(ctx)) {
            if (!out.contains(p) && !dismissed.contains(p)
                    && ctx.getPackageManager().getLaunchIntentForPackage(p) != null) out.add(p);
        }
        // Gerade geoeffnete App SOFORT vorne (nach den angepinnten) - der
        // UsageStatsManager verbucht das In-den-Vordergrund-Kommen erst mit ein
        // paar Sekunden Verzoegerung. Ohne das rendert der volle Neuaufbau, der
        // im schonenden/Hybrid-Modus GENAU beim Oeffnen laeuft, noch die alte
        // Reihenfolge - und die bleibt dort haengen, weil die nachfolgenden
        // schonenden Auffrischungen auf dem BlackBerry Launcher nicht umsortieren.
        String justOpened = Settings.lastOpened(ctx);
        String launcher = launcherPkg(ctx);
        if (justOpened != null && !out.contains(justOpened) && !dismissed.contains(justOpened)
                && !justOpened.equals(ctx.getPackageName())
                && !justOpened.equals(launcher)
                && ctx.getPackageManager().getLaunchIntentForPackage(justOpened) != null) {
            out.add(justOpened);
        }
        for (String p : UsageProvider.recentPackages(ctx, max * 2 + 4)) {
            if (out.size() >= max) break;
            if (!out.contains(p) && !dismissed.contains(p)) out.add(p);
        }
        while (out.size() > max) out.remove(out.size() - 1);
        return out;
    }

    /** Kachelhoehe in dp. Automatik: aus Widget-Breite, Spaltenzahl und
     *  Bildschirm-Seitenverhaeltnis. Sonst der manuell eingestellte Wert. */
    static int tileHeightDp(Context ctx, AppWidgetManager mgr, int id) {
        if (!Settings.autoHeight(ctx)) return Settings.tileHeightDp(ctx);
        try {
            Bundle opt = mgr.getAppWidgetOptions(id);
            int widthDp = 0;
            if (opt != null) {
                widthDp = opt.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 0);
                if (widthDp <= 0) widthDp = opt.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0);
            }
            if (widthDp <= 0) return Settings.tileHeightDp(ctx);
            int columns = Math.max(1, Settings.columns(ctx));
            int colWidthDp = (widthDp - (columns - 1) * 14) / columns;
            if (colWidthDp < 40) colWidthDp = 40;
            android.util.DisplayMetrics dm = ctx.getResources().getDisplayMetrics();
            float aspect = (float) Math.max(dm.widthPixels, dm.heightPixels)
                    / Math.max(1, Math.min(dm.widthPixels, dm.heightPixels));
            int h = Math.round(colWidthDp * aspect);
            if (h < 60) h = 60;
            if (h > 360) h = 360;
            return h;
        } catch (Throwable t) {
            return Settings.tileHeightDp(ctx);
        }
    }

    static String launcherPkg(Context ctx) {
        try {
            Intent home = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
            android.content.pm.ResolveInfo ri = ctx.getPackageManager()
                    .resolveActivity(home, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY);
            if (ri != null && ri.activityInfo != null) return ri.activityInfo.packageName;
        } catch (Throwable ignored) {}
        return null;
    }
}
