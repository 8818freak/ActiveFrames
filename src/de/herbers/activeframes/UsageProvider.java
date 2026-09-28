package de.herbers.activeframes;

import android.app.AppOpsManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Process;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Liefert die zuletzt benutzten Apps in MRU-Reihenfolge (zuletzt geoeffnet
 *  zuerst) ueber den UsageStatsManager - genau die BB-Reihenfolge: oben links
 *  die zuletzt verwendete App, dann die davor, usw. Braucht den einmaligen
 *  Nutzungsdatenzugriff (eigener System-Bildschirm, kein Laufzeit-Dialog). */
final class UsageProvider {

    private UsageProvider() {}

    static boolean hasAccess(Context c) {
        try {
            AppOpsManager ao = (AppOpsManager) c.getSystemService(Context.APP_OPS_SERVICE);
            int mode = ao.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(), c.getPackageName());
            return mode == AppOpsManager.MODE_ALLOWED;
        } catch (Throwable t) {
            return false;
        }
    }

    /** Startbare Pakete, nach letztem In-den-Vordergrund-Zeitpunkt absteigend. */
    static List<String> recentPackages(Context c, int limit) {
        List<String> out = new ArrayList<>();
        try {
            UsageStatsManager usm = (UsageStatsManager) c.getSystemService(Context.USAGE_STATS_SERVICE);
            long end = System.currentTimeMillis();
            long begin = end - 7L * 24 * 60 * 60 * 1000;
            UsageEvents events = usm.queryEvents(begin, end);
            // Letzten Vordergrund-Zeitpunkt je Paket sammeln.
            Map<String, Long> lastFg = new LinkedHashMap<>();
            UsageEvents.Event e = new UsageEvents.Event();
            while (events.hasNextEvent()) {
                events.getNextEvent(e);
                int t = e.getEventType();
                if (t == UsageEvents.Event.ACTIVITY_RESUMED || t == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                    String pkg = e.getPackageName();
                    Long prev = lastFg.get(pkg);
                    if (prev == null || e.getTimeStamp() > prev) lastFg.put(pkg, e.getTimeStamp());
                }
            }
            // Nach Zeit absteigend sortieren.
            List<Map.Entry<String, Long>> entries = new ArrayList<>(lastFg.entrySet());
            entries.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));

            PackageManager pm = c.getPackageManager();
            String self = c.getPackageName();
            String launcher = defaultLauncher(c); // Startbildschirm selbst nicht als "App" zeigen
            for (Map.Entry<String, Long> entry : entries) {
                String pkg = entry.getKey();
                if (pkg.equals(self)) continue;                     // uns selbst nicht zeigen
                if (pkg.equals(launcher)) continue;                 // Launcher/Home nicht zeigen
                if (pm.getLaunchIntentForPackage(pkg) == null) continue; // nur startbare Apps
                out.add(pkg);
                if (out.size() >= limit) break;
            }
        } catch (Throwable ignored) {}
        return out;
    }

    private static String defaultLauncher(Context c) {
        try {
            android.content.Intent home = new android.content.Intent(android.content.Intent.ACTION_MAIN)
                    .addCategory(android.content.Intent.CATEGORY_HOME);
            android.content.pm.ResolveInfo ri = c.getPackageManager()
                    .resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY);
            if (ri != null && ri.activityInfo != null) return ri.activityInfo.packageName;
        } catch (Throwable ignored) {}
        return null;
    }
}
