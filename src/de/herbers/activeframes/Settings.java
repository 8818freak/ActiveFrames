package de.herbers.activeframes;

import android.content.Context;
import android.content.SharedPreferences;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Alle Einstellungen der Kacheln in einer SharedPreferences-Datei. Bewusst
 *  schlank: Spaltenzahl, Kachelhoehe (fuers Seitenverhaeltnis - BB machte die
 *  Kacheln rechteckig wie der Bildschirm, nicht immer quadratisch), Anzahl der
 *  Kacheln, Namen ein/aus und die angepinnten Apps. */
public final class Settings {

    private static final String PREFS = "active_frames";
    private static final String K_COLUMNS = "columns";
    private static final String K_TILE_HEIGHT = "tile_height_dp";
    private static final String K_MAX_TILES = "max_tiles";
    private static final String K_SHOW_LABELS = "show_labels";
    private static final String K_AUTO_HEIGHT = "auto_height"; // Kachelhoehe = Bildschirmformat
    private static final String K_SCROLL = "scroll"; // scrollbare Sammlung statt fester Raster
    private static final String K_GENTLE = "gentle_refresh"; // schonendes Auffrischen (moderne Launcher)
    private static final String K_GENTLE_FULLCLOSE = "gentle_full_close"; // Hybrid: Schließen voll auffrischen
    private static final String K_BIG_ROWS = "big_rows"; // wie viele obere Reihen in voller Hoehe
    private static final String K_SHORT_PCT = "short_row_pct"; // Hoehe der folgenden Reihen in %
    private static final String K_PINNED = "pinned"; // Reihenfolge zaehlt -> \n-getrennt
    private static final String K_LAST_OPENED = "last_opened_pkg"; // zuletzt geoeffnete App (Paket)
    private static final String K_LAST_OPENED_AT = "last_opened_at"; // Zeitpunkt dazu

    private Settings() {}

    private static SharedPreferences p(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    // Standard: 2 Spalten hochkanter Kacheln - wie BlackBerry OS10 (dort zwei
    // Spalten bildschirmproportionaler Frames). Max 6 = Anzahl der Kachel-
    // Plaetze je Zeile in row.xml.
    public static final int MAX_COLUMNS = 6;
    public static int columns(Context c) { return clamp(p(c).getInt(K_COLUMNS, 2), 1, MAX_COLUMNS); }
    public static void setColumns(Context c, int n) { p(c).edit().putInt(K_COLUMNS, clamp(n, 1, MAX_COLUMNS)).apply(); }

    // Variable Reihenhoehen (wie BB OS10: obere Reihen gross, danach kleiner).
    // bigRows = wie viele Reihen oben in voller Hoehe bleiben; ab der Reihe
    // danach gelten shortRowPercent der vollen Hoehe. bigRows=1 => "ab der
    // zweiten Reihe kleiner". bigRows sehr gross => alle gleich hoch.
    public static int bigRows(Context c) { return clamp(p(c).getInt(K_BIG_ROWS, 1), 0, 20); }
    public static void setBigRows(Context c, int n) { p(c).edit().putInt(K_BIG_ROWS, clamp(n, 0, 20)).apply(); }
    public static int shortRowPercent(Context c) { return clamp(p(c).getInt(K_SHORT_PCT, 60), 20, 100); }
    public static void setShortRowPercent(Context c, int n) { p(c).edit().putInt(K_SHORT_PCT, clamp(n, 20, 100)).apply(); }

    /** Hoehe einer Kachel in dp - zusammen mit der (vom Raster bestimmten)
     *  Breite ergibt das das Seitenverhaeltnis. Standard hochkant (Bildschirm-
     *  format); kleiner = mehr Kacheln sichtbar. */
    public static int tileHeightDp(Context c) { return p(c).getInt(K_TILE_HEIGHT, 176); }
    public static void setTileHeightDp(Context c, int dp) { p(c).edit().putInt(K_TILE_HEIGHT, clamp(dp, 60, 360)).apply(); }

    /** Wenn an, wird die Kachelhoehe automatisch aus Widget-Breite, Spaltenzahl
     *  und Bildschirm-Seitenverhaeltnis berechnet (Kacheln bekommen das
     *  Bildschirmformat). Wenn aus, gilt die manuelle Hoehe oben. */
    public static boolean autoHeight(Context c) { return p(c).getBoolean(K_AUTO_HEIGHT, true); }
    public static void setAutoHeight(Context c, boolean on) { p(c).edit().putBoolean(K_AUTO_HEIGHT, on).apply(); }

    /** Scrollbare Sammlung (mehr Kacheln als sichtbar, aber auf ALTEN Launchern
     *  fehlerhaftes Kachel-Recycling) statt des festen, immer korrekten Rasters
     *  mit variablen Reihenhoehen. Standard AUS (sicher). */
    public static boolean scroll(Context c) { return p(c).getBoolean(K_SCROLL, false); }
    public static void setScroll(Context c, boolean on) { p(c).edit().putBoolean(K_SCROLL, on).apply(); }

    /** Schonendes Auffrischen der Sammlung (nur Scroll-Modus): statt des vollen
     *  Neuaufbaus (updateAppWidget) nur notifyAppWidgetViewDataChanged. Auf
     *  MODERNEN Launchern verhindert das das Bild-"Springen" beim Schließen; der
     *  BlackBerry Launcher ignoriert diesen Weg jedoch (dann aktualisiert sich
     *  die Sammlung nicht - Schließen wirkungslos). Standard AUS (sicher). */
    public static boolean gentleRefresh(Context c) { return p(c).getBoolean(K_GENTLE, false); }
    public static void setGentleRefresh(Context c, boolean on) { p(c).edit().putBoolean(K_GENTLE, on).apply(); }

    /** Hybrid (nur bei „Schonend auffrischen"): Öffnen/Scrollen/Benachrichti-
     *  gungen schonend (flüssig), aber das Schließen (✕) nimmt den vollen
     *  updateAppWidget-Weg. Das ist auf Launchern, die das schonende Auffrischen
     *  nur unzuverlässig annehmen (BlackBerry Launcher), zuverlässig UND weckt
     *  die Sammlung wieder auf, sodass das schonende Auffrischen danach wieder
     *  greift. Preis: das ✕ ruckelt kurz. Standard AUS (auf modernen Launchern
     *  ist reines Schonen besser - dort ruckelt gar nichts). */
    public static boolean gentleFullClose(Context c) { return p(c).getBoolean(K_GENTLE_FULLCLOSE, false); }
    public static void setGentleFullClose(Context c, boolean on) { p(c).edit().putBoolean(K_GENTLE_FULLCLOSE, on).apply(); }

    public static int maxTiles(Context c) { return p(c).getInt(K_MAX_TILES, 12); }
    public static void setMaxTiles(Context c, int n) { p(c).edit().putInt(K_MAX_TILES, clamp(n, 4, 100)).apply(); }

    public static boolean showLabels(Context c) { return p(c).getBoolean(K_SHOW_LABELS, true); }
    public static void setShowLabels(Context c, boolean on) { p(c).edit().putBoolean(K_SHOW_LABELS, on).apply(); }

    public static List<String> pinned(Context c) {
        String raw = p(c).getString(K_PINNED, "");
        List<String> out = new ArrayList<>();
        if (raw != null && !raw.isEmpty()) {
            for (String s : raw.split("\n")) if (!s.isEmpty()) out.add(s);
        }
        return out;
    }
    public static boolean isPinned(Context c, String pkg) { return pinned(c).contains(pkg); }
    public static void setPinned(Context c, List<String> pkgs) {
        p(c).edit().putString(K_PINNED, String.join("\n", pkgs)).apply();
    }
    public static void togglePinned(Context c, String pkg) {
        List<String> l = pinned(c);
        if (!l.remove(pkg)) l.add(pkg);
        setPinned(c, l);
    }

    /** Die gerade vom Nutzer geoeffnete App (per Kachel, aus dem Launcher oder
     *  aus der EdgeTab-Karte) - damit orderedPkgs sie SOFORT oben links zeigen
     *  kann, ohne auf den UsageStatsManager zu warten. Der hinkt beim In-den-
     *  Vordergrund-Kommen ein paar Sekunden nach; im schonenden/Hybrid-Modus,
     *  wo der volle (einzig umsortierende) Neuaufbau genau beim Oeffnen laeuft,
     *  landete die frisch geoeffnete App sonst nicht zuverlaessig vorne. */
    public static void setLastOpened(Context c, String pkg) {
        if (pkg == null) return;
        p(c).edit().putString(K_LAST_OPENED, pkg)
                .putLong(K_LAST_OPENED_AT, System.currentTimeMillis()).apply();
    }

    /** Zuletzt geoeffnete App, aber nur wenn das noch nicht lange her ist -
     *  danach uebernimmt ohnehin der UsageStatsManager (der bis dahin
     *  nachgezogen hat), und ein alter Wert soll die echte MRU-Reihenfolge
     *  nicht dauerhaft verfaelschen. */
    public static String lastOpened(Context c) {
        long at = p(c).getLong(K_LAST_OPENED_AT, 0);
        if (at <= 0 || System.currentTimeMillis() - at > 120_000L) return null;
        return p(c).getString(K_LAST_OPENED, null);
    }

    private static int clamp(int v, int lo, int hi) { return v < lo ? lo : (v > hi ? hi : v); }

    // ---- Sichern/Wiederherstellen -------------------------------------------
    // Genereller Dump/Restore der GESAMTEN Einstellungen (Spalten, Kachelhoehe,
    // Scrollen, Anzahl, Reihen, angepinnte Apps ...) als Text zum Kopieren -
    // analog zu EdgeTab. Die Kachelbilder/Sterne (Cache/Zustand) sind bewusst
    // NICHT Teil der Sicherung: sie bauen sich von selbst wieder auf.
    private static final String BACKUP_HEADER = "ActiveFrames-Backup 1";

    // Delegiert an die gemeinsame Bibliothek (de.herbers.common.SettingsBackup,
    // Git-Submodul common/) - dieselbe Logik in EdgeTab/Sucher/ActiveFrames,
    // eine Quelle der Wahrheit.
    public static String exportText(Context c) {
        return de.herbers.common.SettingsBackup.export(p(c), BACKUP_HEADER);
    }

    /** Ersetzt den GESAMTEN Einstellungsstand durch eine Sicherung. Liefert
     *  false bei erkennbar falschem/beschaedigtem Format, ohne etwas zu aendern. */
    public static boolean importText(Context c, String text) {
        return de.herbers.common.SettingsBackup.importInto(p(c), BACKUP_HEADER, text);
    }
}
