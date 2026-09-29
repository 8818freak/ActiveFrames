package de.herbers.activeframes;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

import de.herbers.common.PermReminder;

import java.util.ArrayList;
import java.util.List;

/** Eine Quelle fuer die Berechtigungen dieser App - genutzt fuer die Erinnerung
 *  bei Verlust (PermReminder) UND fuer den aufklappbaren Berechtigungs-Abschnitt
 *  in den Einstellungen. */
final class Perms {
    private Perms() {}

    static List<PermReminder.Perm> list(Context ctx) {
        List<PermReminder.Perm> l = new ArrayList<>();
        l.add(new PermReminder.Perm("accessibility", "Bedienungshilfe (Kachel-Fotos)",
                MainActivity.accAccess(ctx), new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        l.add(new PermReminder.Perm("notif", "Benachrichtigungszugriff",
                MainActivity.notifAccess(ctx), new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));
        l.add(new PermReminder.Perm("usage", "Nutzungszugriff (App-Reihenfolge)",
                UsageProvider.hasAccess(ctx), new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)));
        return l;
    }

    static void checkReminders(Context ctx) {
        PermReminder.check(ctx, "Active Frames", list(ctx));
    }
}
