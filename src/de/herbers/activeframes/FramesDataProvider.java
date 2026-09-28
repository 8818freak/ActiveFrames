package de.herbers.activeframes;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

/** Datenkanal fuer ein zweites, NATIVES Active-Frames-Widget in einer anderen
 *  App des Nutzers (EdgeTab, "Widget 2"). Liefert dieselbe Kachel-Reihenfolge,
 *  dieselben Sterne und Bilder wie das Home-Screen-Widget.
 *
 *  Warum ueber diesen Kanal statt EdgeTab eigene Rechte zu geben: Active Frames
 *  hat bereits Nutzungsdaten-, Benachrichtigungs- und (optional) Bedienungs-
 *  hilfe-Zugriff und ist damit die einzige Datenquelle. EdgeTab muss KEINE
 *  dieser heiklen Berechtigungen anfordern und bekommt die echten App-Fotos
 *  "geschenkt". Weil EdgeTab die Kacheln in seiner eigenen Leiste selbst
 *  zeichnet (kein Launcher-Host), gibt es dort auch kein fehlerhaftes
 *  Kachel-Recycling - das Scrollen wird zuverlaessig.
 *
 *  Exportiert, aber streng: nur der Nutzer-eigene EdgeTab (und Active Frames
 *  selbst) darf lesen; jeder andere Aufrufer wird abgewiesen. Den Aufrufer
 *  meldet das System ueber getCallingPackage() sicher (nicht faelschbar), es
 *  ist also keine gesonderte Berechtigung noetig. */
public class FramesDataProvider extends ContentProvider {

    static final String AUTHORITY = "de.herbers.activeframes.data";
    private static final String ALLOWED_APP = "de.herbers.edgetab";

    /** Nur EdgeTab und wir selbst duerfen zugreifen. */
    private boolean allowed() {
        String caller = getCallingPackage();
        if (caller == null) return false;
        if (caller.equals(ALLOWED_APP)) return true;
        Context c = getContext();
        return c != null && caller.equals(c.getPackageName());
    }

    @Override public boolean onCreate() { return true; }

    /** content://de.herbers.activeframes.data/tiles -> Kacheln in Reihenfolge.
     *  Spalten: pkg (Paketname), unread (1/0 = roter Stern), image (Dateiname
     *  des Kachelbilds oder leer). */
    @Override
    public Cursor query(Uri uri, String[] projection, String sel, String[] selArgs, String order) {
        if (!allowed() || getContext() == null) return null;
        List<String> seg = uri.getPathSegments();
        if (seg.isEmpty() || !"tiles".equals(seg.get(0))) return null;
        Context ctx = getContext();
        MatrixCursor cur = new MatrixCursor(new String[]{"pkg", "unread", "image"});
        for (String pkg : FramesWidget.orderedPkgs(ctx)) {
            String img = ImageStore.hasImage(ctx, pkg) ? ImageStore.fileNameFor(pkg) : "";
            cur.addRow(new Object[]{pkg, ImageStore.isUnread(ctx, pkg) ? 1 : 0, img});
        }
        return cur;
    }

    /** content://de.herbers.activeframes.data/image/<dateiname> -> das PNG. */
    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (!allowed() || getContext() == null) throw new FileNotFoundException();
        List<String> seg = uri.getPathSegments();
        if (seg.size() < 2 || !"image".equals(seg.get(0))) throw new FileNotFoundException();
        String name = seg.get(1);
        File dir = new File(getContext().getCacheDir(), "frames");
        File f = new File(dir, name);
        try {
            // Nur Dateien innerhalb des frames-Verzeichnisses zulassen.
            if (!f.getCanonicalPath().startsWith(dir.getCanonicalPath() + File.separator)) {
                throw new FileNotFoundException();
            }
        } catch (Exception e) {
            throw new FileNotFoundException();
        }
        if (!f.exists()) throw new FileNotFoundException();
        return ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY);
    }

    /** Zustandsaenderungen von der fremden App zurueckmelden, damit der Stern-/
     *  Verworfen-Zustand in Active Frames dieselbe Quelle bleibt:
     *   - "opened":  App wurde aus der EdgeTab-Karte geoeffnet -> Stern weg,
     *                eine zuvor geschlossene Kachel wieder einblenden.
     *   - "dismiss": Kachel in der EdgeTab-Karte geschlossen (X) -> verwerfen. */
    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        if (!allowed() || getContext() == null || arg == null) return null;
        Context ctx = getContext();
        if ("opened".equals(method)) {
            ImageStore.clearUnread(ctx, arg);
            ImageStore.clearDismissed(ctx, arg);
            FramesWidget.refreshAfterAction(ctx);
        } else if ("dismiss".equals(method)) {
            ImageStore.markDismissed(ctx, arg);
            FramesWidget.refreshAfterAction(ctx);
        }
        return null;
    }

    @Override public String getType(Uri uri) { return "vnd.android.cursor.dir/vnd.de.herbers.activeframes.tile"; }
    @Override public Uri insert(Uri u, ContentValues v) { return null; }
    @Override public int delete(Uri u, String s, String[] a) { return 0; }
    @Override public int update(Uri u, ContentValues v, String s, String[] a) { return 0; }
}
