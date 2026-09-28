package de.herbers.activeframes;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import java.util.ArrayList;
import java.util.List;

/** Kachel-Nachschub für den optionalen Scroll-Modus (GridView-Sammlung).
 *  ACHTUNG: Auf alten Launchern recycelt die Sammlung Kacheln fehlerhaft
 *  (Bild/Name können verrutschen) - deshalb ist der Scroll-Modus optional und
 *  mit Warnhinweis versehen; die feste Variante (FramesWidget.updateWidget)
 *  ist immer korrekt. */
class FramesRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {

    private final Context ctx;
    private final int appWidgetId;
    private volatile List<String> pkgs = new ArrayList<>();

    FramesRemoteViewsFactory(Context ctx, int appWidgetId) {
        this.ctx = ctx;
        this.appWidgetId = appWidgetId;
    }

    @Override public void onCreate() {}
    @Override public void onDestroy() { pkgs = new ArrayList<>(); }
    @Override public void onDataSetChanged() { pkgs = FramesWidget.orderedPkgs(ctx); }

    @Override public int getCount() { return pkgs.size(); }
    @Override public int getViewTypeCount() { return 1; }
    @Override public long getItemId(int position) {
        List<String> l = pkgs;
        return (position >= 0 && position < l.size()) ? l.get(position).hashCode() : position;
    }
    // BEWUSST false: Mit stabilen IDs versucht der BlackBerry Launcher, beim
    // Umsortieren vorhandene Kachel-Ansichten anhand ihrer ID wiederzuverwenden
    // und nur zu verschieben - dabei setzt er die Texte neu, aber die Bitmaps
    // (Symbol/Kachelbild) der wiederverwendeten Ansichten NICHT (beobachtet:
    // Namen stimmten nach dem Öffnen, Symbole/Bilder hingen eine Position
    // hinterher). Ohne stabile IDs baut er jede sichtbare Position frisch aus
    // getViewAt auf -> Symbol, Name und Bild gehören wieder zusammen.
    @Override public boolean hasStableIds() { return false; }
    @Override public RemoteViews getLoadingView() { return null; }

    @Override public RemoteViews getViewAt(int position) {
        RemoteViews t = new RemoteViews(ctx.getPackageName(), R.layout.tile_scroll);
        List<String> l = pkgs;
        if (position < 0 || position >= l.size()) return t;
        String pkg = l.get(position);

        AppWidgetManager mgr = AppWidgetManager.getInstance(ctx);
        int hDp = FramesWidget.tileHeightDp(ctx, mgr, appWidgetId);
        if (Build.VERSION.SDK_INT >= 31) {
            t.setViewLayoutHeight(R.id.tile_root, hDp, TypedValue.COMPLEX_UNIT_DIP);
        }

        // GANZE Kachel als EIN Bild (Kachelbild + Symbol + Name + Stern
        // zusammengezeichnet). Grund: Der BlackBerry Launcher recycelt die
        // Sammlungs-Kacheln fehlerhaft und aktualisiert beim Umsortieren Bild-
        // und Text-Elemente NICHT gemeinsam - dann zeigte eine Kachel z.B.
        // Sucher als Bild, aber Symbol/Name einer anderen App. Steckt alles in
        // EINEM Bitmap, kann nichts mehr auseinanderlaufen: der Launcher zeigt
        // entweder das ganze neue oder das ganze alte Kachelbild, aber nie eine
        // Mischung. Nur das feste ✕ bleibt ein eigenes Element (unveränderlich,
        // kann darum nicht "verrutschen").
        int wPx = tileWidthPx(mgr, hDp);
        int hPx = Math.round(hDp * ctx.getResources().getDisplayMetrics().density);
        Bitmap composite = renderTile(pkg, wPx, hPx);
        if (composite != null) {
            t.setImageViewBitmap(R.id.tile_image, composite);
            t.setViewVisibility(R.id.tile_image, View.VISIBLE);
        } else {
            t.setViewVisibility(R.id.tile_image, View.GONE);
        }
        // Die getrennten Text/Symbol-Elemente NICHT mehr mit Inhalt fuellen
        // (siehe oben). Das Namensfeld bleibt aber als UNSICHTBARER Platzhalter
        // mit Gewicht stehen, damit es das ✕ in der Leiste nach RECHTS schiebt
        // (waere es GONE, verloere die Leiste ihren rechts-Schub und das ✕ saesse
        // links ueber dem eingezeichneten Namen). Leiste transparent -> nur das
        // feste ✕ ist sichtbar, alles andere kommt aus dem Kachelbild.
        t.setViewVisibility(R.id.tile_title, View.GONE);
        t.setViewVisibility(R.id.tile_icon, View.GONE);
        t.setTextViewText(R.id.tile_label, "");
        t.setViewVisibility(R.id.tile_label, View.INVISIBLE);
        t.setViewVisibility(R.id.tile_star, View.GONE);
        t.setInt(R.id.tile_bar, "setBackgroundColor", android.graphics.Color.TRANSPARENT);

        Intent open = new Intent().setData(Uri.parse("af://open/" + pkg))
                .putExtra(FramesWidget.EXTRA_PKG, pkg);
        t.setOnClickFillInIntent(R.id.tile_card, open);
        Intent dismiss = new Intent().setData(Uri.parse("af://dismiss/" + pkg))
                .putExtra(FramesWidget.EXTRA_PKG, pkg).putExtra(FramesWidget.EXTRA_DISMISS, true);
        t.setOnClickFillInIntent(R.id.tile_close, dismiss);
        return t;
    }

    /** Kachelbreite in px (aus Widget-Breite und Spaltenzahl), gedeckelt, damit
     *  das je Kachel uebertragene Bitmap klein bleibt. */
    private int tileWidthPx(AppWidgetManager mgr, int hDp) {
        float density = ctx.getResources().getDisplayMetrics().density;
        int cols = Math.max(1, Settings.columns(ctx));
        int widthDp = 0;
        try {
            android.os.Bundle opt = mgr.getAppWidgetOptions(appWidgetId);
            if (opt != null) {
                widthDp = opt.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 0);
                if (widthDp <= 0) widthDp = opt.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0);
            }
        } catch (Throwable ignored) {}
        int colWidthDp = widthDp > 0 ? Math.max(48, (widthDp - (cols - 1) * 14) / cols) : Math.max(48, hDp * 3 / 4);
        int px = Math.round(colWidthDp * density);
        return Math.max(96, Math.min(px, 360)); // Deckel gegen zu grosse Bitmaps
    }

    /** Zeichnet die komplette Kachel in EIN Bitmap: Kachelbild (oder Logo)
     *  formatfuellend, unten eine dunkle Leiste mit App-Symbol + Name, oben
     *  rechts der rote Stern bei etwas Neuem. Rechts unten bleibt Platz fuer das
     *  (separate) ✕. */
    private Bitmap renderTile(String pkg, int wPx, int hPx) {
        try {
            if (wPx <= 0 || hPx <= 0) return null;
            float density = ctx.getResources().getDisplayMetrics().density;
            Bitmap out = Bitmap.createBitmap(wPx, hPx, Bitmap.Config.ARGB_8888);
            android.graphics.Canvas c = new android.graphics.Canvas(out);
            c.drawColor(android.graphics.Color.parseColor("#22282C"));

            // Kachelbild formatfuellend (center-crop); sonst Logo kachelgross.
            Bitmap image = ImageStore.getImageScaledCached(ctx, pkg, Math.max(wPx, hPx));
            boolean haveImage = image != null;
            if (!haveImage) image = AppInfoCache.logoTileCached(ctx, pkg, wPx, hPx);
            if (image != null) drawCenterCrop(c, image, wPx, hPx);

            android.graphics.Paint p = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);

            // Untere Leiste (nur wenn ein echtes Kachelbild da ist - beim Logo
            // ist die App ohnehin schon klar und eine Leiste stoert nur).
            int barH = Math.round(42 * density);
            int closeW = Math.round(48 * density); // Platz fuer das separate ✕ rechts
            String label = AppInfoCache.label(ctx, pkg);
            if (haveImage) {
                p.setColor(android.graphics.Color.parseColor("#CC000000"));
                c.drawRect(0, hPx - barH, wPx, hPx, p);
                int pad = Math.round(8 * density);
                int iconS = Math.round(22 * density);
                Bitmap icon = AppInfoCache.icon(ctx, pkg, iconS);
                int textLeft = pad;
                if (icon != null) {
                    android.graphics.Rect dst = new android.graphics.Rect(
                            pad, hPx - barH + (barH - iconS) / 2, pad + iconS, hPx - barH + (barH + iconS) / 2);
                    c.drawBitmap(icon, null, dst, p);
                    textLeft = pad + iconS + Math.round(8 * density);
                }
                p.setColor(android.graphics.Color.WHITE);
                p.setTextSize(12 * density);
                int textRight = wPx - closeW;
                String shown = ellipsize(label, p, textRight - textLeft);
                android.graphics.Paint.FontMetrics fm = p.getFontMetrics();
                float baseline = hPx - barH / 2f - (fm.ascent + fm.descent) / 2f;
                c.drawText(shown, textLeft, baseline, p);
            }

            // Roter Stern oben rechts bei etwas Neuem.
            if (ImageStore.isUnread(ctx, pkg)) {
                int starS = Math.round(20 * density);
                int m = Math.round(4 * density);
                drawStar(c, wPx - starS - m, m, starS);
            }
            return out;
        } catch (Throwable t) {
            return null;
        }
    }

    private void drawCenterCrop(android.graphics.Canvas c, Bitmap bmp, int wPx, int hPx) {
        int bw = bmp.getWidth(), bh = bmp.getHeight();
        if (bw <= 0 || bh <= 0) return;
        float scale = Math.max(wPx / (float) bw, hPx / (float) bh);
        float dw = bw * scale, dh = bh * scale;
        float left = (wPx - dw) / 2f, top = (hPx - dh) / 2f;
        android.graphics.RectF dst = new android.graphics.RectF(left, top, left + dw, top + dh);
        c.drawBitmap(bmp, null, dst, new android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG));
    }

    private String ellipsize(String s, android.graphics.Paint p, float maxW) {
        if (s == null) return "";
        if (p.measureText(s) <= maxW) return s;
        String ell = "…";
        int end = s.length();
        while (end > 0 && p.measureText(s.substring(0, end) + ell) > maxW) end--;
        return end <= 0 ? ell : s.substring(0, end) + ell;
    }

    /** Weisser Fuenf-Punkt-Stern auf rotem Kreis (wie ic_star_badge). */
    private void drawStar(android.graphics.Canvas c, int x, int y, int size) {
        android.graphics.Paint p = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        float r = size / 2f, cx = x + r, cy = y + r;
        p.setColor(android.graphics.Color.parseColor("#E53935"));
        c.drawCircle(cx, cy, r, p);
        p.setColor(android.graphics.Color.WHITE);
        android.graphics.Path path = new android.graphics.Path();
        float sr = r * 0.62f, ir = sr * 0.42f;
        for (int i = 0; i < 10; i++) {
            double ang = Math.PI / 2 * -1 + i * Math.PI / 5; // Spitze oben
            float rad = (i % 2 == 0) ? sr : ir;
            float px = cx + (float) (rad * Math.cos(ang));
            float py = cy + (float) (rad * Math.sin(ang));
            if (i == 0) path.moveTo(px, py); else path.lineTo(px, py);
        }
        path.close();
        c.drawPath(path, p);
    }
}
