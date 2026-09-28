package de.herbers.activeframes;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import java.util.concurrent.ConcurrentHashMap;

/** Kleiner Zwischenspeicher fuer App-Namen und -Symbole, damit der
 *  Kachel-Nachschub (RemoteViewsFactory) nicht bei jedem Aufbau erneut den
 *  PackageManager fragen muss. */
final class AppInfoCache {

    private AppInfoCache() {}

    private static final ConcurrentHashMap<String, String> LABELS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Bitmap> ICONS = new ConcurrentHashMap<>(); // ~144px

    static String label(Context c, String pkg) {
        String cached = LABELS.get(pkg);
        if (cached != null) return cached;
        String label = pkg;
        try {
            PackageManager pm = c.getPackageManager();
            label = pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString();
        } catch (Throwable ignored) {}
        LABELS.put(pkg, label);
        return label;
    }

    /** App-Symbol als Bitmap (einmal in ~144px gecacht, bei Bedarf skaliert). */
    static Bitmap icon(Context c, String pkg, int sizePx) {
        Bitmap base = ICONS.get(pkg);
        if (base == null) {
            base = renderIcon(c, pkg, 144);
            if (base != null) ICONS.put(pkg, base);
        }
        if (base == null) return null;
        if (sizePx <= 0 || sizePx == base.getWidth()) return base;
        return Bitmap.createScaledBitmap(base, sizePx, sizePx, true);
    }

    /** Kachel mit dem App-Logo mittig auf dunklem Grund - unverzerrt (Seiten-
     *  verhaeltnis des Symbols bleibt) und mit reichlich Rand, damit es auch bei
     *  zuschneidender Anzeige (Center-Crop) nicht angeschnitten wird. Fuer den
     *  Fall "kein Kachelbild vorhanden" (statt nur des Namens) und fuer Apps,
     *  die Bildschirmfotos unterbinden (dann statt Schwarzbild). */
    static Bitmap logoTile(Context c, String pkg, int w, int h) {
        try {
            Drawable icon = c.getPackageManager().getApplicationIcon(pkg);
            if (icon == null) return null;
            Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bmp);
            canvas.drawColor(Color.parseColor("#22282C"));
            int size = Math.round(Math.min(w, h) * 0.55f);
            int left = (w - size) / 2, top = (h - size) / 2;
            icon.setBounds(left, top, left + size, top + size);
            icon.draw(canvas);
            return bmp;
        } catch (Throwable t) {
            return null;
        }
    }

    private static Bitmap renderIcon(Context c, String pkg, int size) {
        try {
            Drawable d = c.getPackageManager().getApplicationIcon(pkg);
            return toBitmap(d, size);
        } catch (Throwable t) {
            return null;
        }
    }

    static Bitmap toBitmap(Drawable d, int size) {
        if (d == null) return null;
        if (d instanceof BitmapDrawable && ((BitmapDrawable) d).getBitmap() != null) {
            Bitmap b = ((BitmapDrawable) d).getBitmap();
            if (size > 0 && (b.getWidth() != size || b.getHeight() != size)) {
                return Bitmap.createScaledBitmap(b, size, size, true);
            }
            return b;
        }
        int w = size > 0 ? size : Math.max(1, d.getIntrinsicWidth());
        int h = size > 0 ? size : Math.max(1, d.getIntrinsicHeight());
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        d.setBounds(0, 0, w, h);
        d.draw(canvas);
        return bmp;
    }
}
