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
        // Persistenter Speicher (getFilesDir), NICHT Cache: sonst wischt ein
        // OS-Update / "Cache leeren" / SD Maid alle Kachelbilder weg (genau das
        // ist passiert). Bounded durch die Zahl der Apps, also unkritisch.
        File d = new File(c.getFilesDir(), "frames");
        if (!d.exists()) d.mkdirs();
        // Einmalige Migration: evtl. noch vorhandene Bilder aus dem alten
        // Cache-Ordner heruebernehmen (best effort, nur was der Cache-Purge
        // uebrig liess).
        try {
            File old = new File(c.getCacheDir(), "frames");
            File[] olds = old.isDirectory() ? old.listFiles() : null;
            if (olds != null) {
                for (File o : olds) {
                    File t = new File(d, o.getName());
                    if (!t.exists()) {
                        try (java.io.FileInputStream in = new java.io.FileInputStream(o);
                             FileOutputStream out = new FileOutputStream(t)) {
                            byte[] buf = new byte[8192]; int n;
                            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                        }
                    }
                    o.delete();
                }
            }
        } catch (Throwable ignored) {}
        return d;
    }

    /** Der Ordner mit allen Kachelbildern (und Logos) - fuer die Sicherung, die
     *  diese Bilder mitnimmt (siehe Backup). */
    static File framesDir(Context c) { return dir(c); }

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

    private static String logoFileName(String pkg) {
        return "logo_" + pkg.replaceAll("[^A-Za-z0-9._-]", "_") + ".png";
    }

    private static File logoFile(Context c, String pkg) {
        return new File(dir(c), logoFileName(pkg));
    }

    /** content://-Adresse einer Logo-Kachel (App-Symbol mittig auf dunklem
     *  Grund), bei Bedarf einmalig erzeugt. Fuer den festen Layout-Modus, wo
     *  ALLE Kacheln in EINER RemoteViews stecken und grosse Bitmaps den Binder
     *  sprengen - das Logo kommt daher (wie die echten Bilder) per URI. */
    static android.net.Uri logoUriFor(Context c, String pkg) {
        File f = logoFile(c, pkg);
        if (!f.exists()) {
            Bitmap logo = AppInfoCache.logoTile(c, pkg, 300, 400);
            if (logo != null) {
                try (FileOutputStream out = new FileOutputStream(f)) {
                    logo.compress(Bitmap.CompressFormat.PNG, 90, out);
                } catch (Throwable ignored) {}
            }
        }
        if (!f.exists()) return null;
        return new android.net.Uri.Builder()
                .scheme("content").authority(FrameImageProvider.AUTHORITY)
                .appendPath(logoFileName(pkg))
                .appendQueryParameter("t", String.valueOf(f.lastModified()))
                .build();
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
            // Quellbild mit etwas höherer Auflösung ablegen (längste Kante),
            // damit große Kacheln - vor allem die native EdgeTab-Karte
            // (Widget 2) - schärfer sind. Das Home-Widget skaliert daraus je
            // nach Kachelzahl passend klein herunter (Binder-Grenze).
            Bitmap scaled = scaleDown(bmp, 400);
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

    /** Kachelbild klein skaliert - fuer den Scroll-Modus (Sammlung), wo jede
     *  Zeile ihr eigenes Bitmap per setImageViewBitmap traegt (wie BlackBerrys
     *  Hub-Widget). Kleine Bitmaps, damit die (batchweise) uebertragenen Zeilen
     *  den Binder nicht sprengen. */
    static Bitmap getImageScaled(Context c, String pkg, int maxEdge) {
        Bitmap b = getImage(c, pkg);
        return b != null ? scaleDown(b, maxEdge) : null;
    }

    // In-Memory-Cache der verkleinerten Kachelbilder. Die Sammlung (Scroll-
    // Modus) baut sich beim Auffrischen - z.B. nach dem Schliessen einer Kachel
    // - komplett neu auf; ohne Cache dekodiert getViewAt jedes sichtbare Bild
    // erneut von der Platte, was die Neubindung verzoegert und das "Springen"
    // verlaengert. Mit Cache ist die Neubindung schnell, das Springen kurz.
    private static final android.util.LruCache<String, Bitmap> SCALED_CACHE =
            new android.util.LruCache<String, Bitmap>(8 * 1024 * 1024) {
                @Override protected int sizeOf(String key, Bitmap b) {
                    return b == null ? 0 : b.getByteCount();
                }
            };

    /** Wie getImageScaled, aber aus dem Speicher-Cache (Schluessel: Paket +
     *  Groesse + Dateizeit - aendert sich das Bild, wird automatisch neu
     *  dekodiert). */
    static Bitmap getImageScaledCached(Context c, String pkg, int maxEdge) {
        File f = file(c, pkg);
        if (!f.exists()) return null;
        String key = pkg + '@' + maxEdge + '@' + f.lastModified();
        Bitmap cached = SCALED_CACHE.get(key);
        if (cached != null && !cached.isRecycled()) return cached;
        Bitmap b = getImageScaled(c, pkg, maxEdge);
        if (b != null) SCALED_CACHE.put(key, b);
        return b;
    }

    /** Alle gespeicherten Kachelbilder UND Logo-Kacheln loeschen (der Speicher-
     *  Cache wird geleert). Kacheln zeigen danach wieder nur das App-Symbol und
     *  bauen sich beim naechsten Oeffnen/bei neuen Benachrichtigungen neu auf. */
    static void clearAllImages(Context c) {
        try {
            File[] files = dir(c).listFiles();
            if (files != null) for (File f : files) { try { f.delete(); } catch (Throwable ignored) {} }
        } catch (Throwable ignored) {}
        try { SCALED_CACHE.evictAll(); } catch (Throwable ignored) {}
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
