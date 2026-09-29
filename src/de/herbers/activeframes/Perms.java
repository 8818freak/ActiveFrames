package de.herbers.activeframes;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

import de.herbers.common.PermReminder;

import java.util.ArrayList;
import java.util.List;

/** Eine Quelle fuer die Berechtigungen dieser App - genutzt fuer die Erinnerung
 *  bei Verlust (PermReminder) UND fuer den aufklappbaren, erklaerten
 *  Berechtigungs-Abschnitt in den Einstellungen. */
final class Perms {
    private Perms() {}

    static List<PermReminder.Perm> list(Context ctx) {
        List<PermReminder.Perm> l = new ArrayList<>();
        l.add(new PermReminder.Perm("usage", "Nutzungszugriff",
                UsageProvider.hasAccess(ctx), new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS),
                "Für die Reihenfolge der Kacheln – welche App zuletzt vorne war."));
        l.add(new PermReminder.Perm("notif", "Benachrichtigungszugriff",
                MainActivity.notifAccess(ctx), new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS),
                "Für den roten Stern bei Neuem und das Kachelbild aus der letzten Benachrichtigung."));
        l.add(new PermReminder.Perm("accessibility", "Bedienungshilfe (echte App-Fotos)",
                MainActivity.accAccess(ctx), new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS),
                "Optional: fotografiert beim Öffnen einer App ihren Zustand für die Kachel "
                + "(echte Active-Frames-Optik). Ohne dies zeigt die Kachel das letzte "
                + "Benachrichtigungsbild bzw. das App-Symbol. Sichere Apps (Banking) bleiben schwarz."));
        return l;
    }

    static void checkReminders(Context ctx) {
        PermReminder.check(ctx, "Active Frames", list(ctx));
    }
}
