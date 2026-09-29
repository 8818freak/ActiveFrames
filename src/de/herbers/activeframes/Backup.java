package de.herbers.activeframes;

import android.content.Context;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Sichert/liest Einstellungen UND die Kachelbilder (die PNGs im frames-Ordner:
 * letztes Benachrichtigungsbild je App plus erzeugte Logo-Kacheln) als EINE
 * Zip-Datei. Ein reiner Text-Export der Einstellungen wuerde die Bilder nicht
 * mitnehmen; nach einer Neuinstallation waeren die Kacheln bis zur naechsten
 * Benachrichtigung leer. Die Bilder kosten kaum Platz (eine pro App).
 *
 * Kein externes Zip-Format/-Bibliothek noetig - java.util.zip ist Teil des JDK.
 * Der Import erkennt auch aeltere, reine Text-Sicherungen (nur Einstellungen).
 */
final class Backup {

    private static final String ENTRY_SETTINGS = "settings.txt";
    private static final String DIR_IMAGES = "frames/";

    private Backup() {}

    static void exportZip(Context ctx, OutputStream out) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(out)) {
            zos.putNextEntry(new ZipEntry(ENTRY_SETTINGS));
            zos.write(Settings.exportText(ctx).getBytes("UTF-8"));
            zos.closeEntry();

            File dir = ImageStore.framesDir(ctx);
            File[] files = dir == null ? null : dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (!f.isFile()) continue;
                    zos.putNextEntry(new ZipEntry(DIR_IMAGES + f.getName()));
                    try (FileInputStream in = new FileInputStream(f)) { copy(in, zos); }
                    zos.closeEntry();
                }
            }
        }
    }

    /** Erkennt Zip (Einstellungen + Bilder) und alte reine Text-Sicherung. */
    static boolean importAuto(Context ctx, InputStream in) throws IOException {
        byte[] all = readAll(in);
        boolean isZip = all.length >= 2 && all[0] == 'P' && all[1] == 'K';
        if (isZip) return importZip(ctx, new ByteArrayInputStream(all));
        return Settings.importText(ctx, new String(all, "UTF-8"));
    }

    static boolean importZip(Context ctx, InputStream in) throws IOException {
        String settingsText = null;
        File dir = ImageStore.framesDir(ctx);
        java.util.List<File> written = new java.util.ArrayList<>();
        java.util.List<byte[]> pending = new java.util.ArrayList<>();
        java.util.List<String> pendingNames = new java.util.ArrayList<>();
        try (ZipInputStream zis = new ZipInputStream(in)) {
            ZipEntry e;
            while ((e = zis.getNextEntry()) != null) {
                String name = e.getName();
                if (ENTRY_SETTINGS.equals(name)) {
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    copy(zis, bos);
                    settingsText = bos.toString("UTF-8");
                } else if (name.startsWith(DIR_IMAGES) && !e.isDirectory()) {
                    // Dateinamen absichern (kein Pfad-Ausbruch).
                    String base = new File(name).getName();
                    if (base.isEmpty()) continue;
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    copy(zis, bos);
                    pendingNames.add(base);
                    pending.add(bos.toByteArray());
                }
            }
        }
        if (settingsText == null) return false; // falsches/beschaedigtes Format
        boolean ok = Settings.importText(ctx, settingsText);
        if (!ok) return false;
        // Bilder erst nach erfolgreichen Einstellungen schreiben.
        if (dir != null && !dir.exists()) dir.mkdirs();
        for (int i = 0; i < pendingNames.size(); i++) {
            File f = new File(dir, pendingNames.get(i));
            try (FileOutputStream fos = new FileOutputStream(f)) { fos.write(pending.get(i)); }
            written.add(f);
        }
        return true;
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        copy(in, bos);
        return bos.toByteArray();
    }

    private static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
    }
}
