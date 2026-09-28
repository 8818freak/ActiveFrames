package de.herbers.activeframes;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashSet;
import java.util.Set;

/** Merkt sich je App das Bild der letzten Benachrichtigung (fuer die Kachel)
 *  und ob es dort etwas Neues gibt (fuer den roten Stern). Das Bild ueberlebt
 *  das "Gelesen"-Markieren - die Kachel zeigt weiter den letzten Inhalt, wie
 *  BlackBerrys Active Frames; nur der Stern verschwindet beim Oeffnen. */
final class ImageStore {

    private ImageStore() {}

    private static final String STATE = "frames_state";
    private static final String K_UNREAD = "unread";
    private static final String K_DISMISSED = "dismissed";

    private static File dir(Context c) {
        File d = new File(c.getCacheDir(), "frames");
        if (!d.exists()) d.mkdirs();
        return d;
    }

    private static String fileName(String pkg) {
        return pkg.replaceAll("[^A-Za-z0-9._-]", "_") + ".png";
    }

    /** Dateiname des Kachelbilds je Paket - fuer den Datenkanal an ein zweites,
     *  natives Widget in einer anderen App (EdgeTab, siehe FramesDataProvider). */
    static String fileNameFor(String pkg) { return fileName(pkg); }

    private static File file(Context c, String pkg) {
        return new File(dir(c), fileName(pkg));
    }

    static boolean hasImage(Context c, String pkg) {
        return file(c, pkg).exists();
    }

    /** content://-Adresse des Kachelbilds (vom Launcher per URI geladen, damit
     *  keine großen Bitmaps durch die begrenzte RemoteViews-Übertragung müssen).
     *  Zeitstempel als Query-Parameter, damit der Launcher ein geändertes Bild
     *  nicht aus seinem Cache nimmt. */
    static android.net.Uri uriFor(Context c, String pkg) {
        File f = file(c, pkg);
        return new android.net.Uri.Builder()
                .scheme("content").authority(FrameImageProvider.AUTHORITY)
                .appendPath(fileName(pkg))
                .appendQueryParameter("t", String.valueOf(f.lastModified()))
                .build();
    }

    static void putImage(Context c, String pkg, Bitmap bmp) {
        if (bmp == null) return;
        try (FileOutputStream out = new FileOutputStream(file(c, pkg))) {
            // moderat herunterskalieren - grosse Bitmaps sprengen sonst die
            // Binder-Transaktion, mit der RemoteViews ans Widget wandern.
            Bitmap scaled = scaleDown(bmp, 260);
            scaled.compress(Bitmap.CompressFormat.PNG, 90, out);
        } catch (Throwable ignored) {}
    }

    static Bitmap getImage(Context c, String pkg) {
        try {
            File f = file(c, pkg);
            if (!f.exists()) return null;
            return BitmapFactory.decodeFile(f.getAbsolutePath());
        } catch (Throwable t) {
            return null;
        }
    }

    private static Bitmap scaleDown(Bitmap b, int maxEdge) {
        int w = b.getWidth(), h = b.getHeight();
        int longest = Math.max(w, h);
        if (longest <= maxEdge) return b;
        float f = maxEdge / (float) longest;
        return Bitmap.createScaledBitmap(b, Math.max(1, (int) (w * f)), Math.max(1, (int) (h * f)), true);
    }

    // ---- "Neues" (roter Stern) ----

    private static SharedPreferences st(Context c) {
        return c.getSharedPreferences(STATE, Context.MODE_PRIVATE);
    }

    static Set<String> unread(Context c) {
        return new HashSet<>(st(c).getStringSet(K_UNREAD, new HashSet<>()));
    }
    static boolean isUnread(Context c, String pkg) { return unread(c).contains(pkg); }
    static void markUnread(Context c, String pkg) {
        Set<String> s = unread(c);
        if (s.add(pkg)) st(c).edit().putStringSet(K_UNREAD, s).apply();
    }
    static void clearUnread(Context c, String pkg) {
        Set<String> s = unread(c);
        if (s.remove(pkg)) st(c).edit().putStringSet(K_UNREAD, s).apply();
    }

    // ---- Verworfene Kacheln (per X geschlossen). Tauchen wieder auf, sobald
    // die App erneut geoeffnet wird oder eine Benachrichtigung schickt. ----

    static Set<String> dismissed(Context c) {
        return new HashSet<>(st(c).getStringSet(K_DISMISSED, new HashSet<>()));
    }
    static void markDismissed(Context c, String pkg) {
        Set<String> s = dismissed(c);
        if (s.add(pkg)) st(c).edit().putStringSet(K_DISMISSED, s).apply();
    }
    static void clearDismissed(Context c, String pkg) {
        Set<String> s = dismissed(c);
        if (s.remove(pkg)) st(c).edit().putStringSet(K_DISMISSED, s).apply();
    }
}
