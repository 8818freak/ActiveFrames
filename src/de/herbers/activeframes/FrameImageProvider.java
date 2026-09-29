package de.herbers.activeframes;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import java.io.File;
import java.io.FileNotFoundException;

/** Liefert die gespeicherten Kachel-Bilder (App-Fotos/Benachrichtigungsbilder)
 *  per content:// an das Widget aus. Grund: RemoteViews-Sammlungen dürfen nur
 *  eine begrenzte Datenmenge übertragen (Binder ~1 MB). Große Bitmaps direkt
 *  ins Widget zu schicken sprengt das bei mehreren Kacheln - dann verrutschen
 *  Bild/Name (falsche Kachel) oder verschwinden. Über eine URI trägt die
 *  RemoteViews nur die Adresse; der Launcher lädt das Bild selbst nach.
 *  Nicht exportiert; der Launcher bekommt per grantUriPermissions Leserecht. */
public class FrameImageProvider extends ContentProvider {

    static final String AUTHORITY = "de.herbers.activeframes.frames";

    @Override public boolean onCreate() { return true; }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        String name = uri.getLastPathSegment();
        if (name == null || getContext() == null) throw new FileNotFoundException();
        File dir = new File(getContext().getFilesDir(), "frames");
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

    @Override public String getType(Uri uri) { return "image/png"; }
    @Override public Cursor query(Uri u, String[] p, String s, String[] a, String o) { return null; }
    @Override public Uri insert(Uri u, ContentValues v) { return null; }
    @Override public int delete(Uri u, String s, String[] a) { return 0; }
    @Override public int update(Uri u, ContentValues v, String s, String[] a) { return 0; }
}
