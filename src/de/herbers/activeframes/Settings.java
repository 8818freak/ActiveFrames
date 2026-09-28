package de.herbers.activeframes;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

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
    private static final String K_BIG_ROWS = "big_rows"; // wie viele obere Reihen in voller Hoehe
    private static final String K_SHORT_PCT = "short_row_pct"; // Hoehe der folgenden Reihen in %
    private static final String K_PINNED = "pinned"; // Reihenfolge zaehlt -> \n-getrennt

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

    private static int clamp(int v, int lo, int hi) { return v < lo ? lo : (v > hi ? hi : v); }
}
