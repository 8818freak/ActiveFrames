package de.herbers.activeframes;

import android.app.Notification;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import de.herbers.common.Notifications;

/** Faengt Benachrichtigungen ab, um (1) den roten Stern zu setzen ("etwas
 *  Neues") und (2) das letzte Bild je App fuer die Kachel zu merken - der
 *  einzige fuer Nicht-System-Apps erreichbare Weg zu "lebendigen" Kacheln:
 *  das Foto/Cover/grosse Symbol aus der letzten Benachrichtigung (Screenshots
 *  fremder Apps sind gesperrt). */
public class FramesNotificationListener extends NotificationListenerService {

    @Override
    public void onListenerConnected() {
        FramesWidget.refreshData(this);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        handle(sbn);
    }

    /** Wird die Benachrichtigung entfernt (weggewischt, vom Nutzer im System
     *  gelöscht oder von der App zurückgezogen), soll der rote Stern erlöschen -
     *  aber nur, wenn KEINE echte Benachrichtigung dieser App mehr aktiv ist
     *  (sonst verschwände der Stern schon, wenn von mehreren nur eine wegfällt). */
    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        try {
            if (sbn == null) return;
            String pkg = sbn.getPackageName();
            if (pkg == null || pkg.equals(getPackageName())) return;
            if (!ImageStore.isUnread(this, pkg)) return;
            if (!hasRealNotification(pkg)) {
                ImageStore.clearUnread(this, pkg);
                FramesWidget.refreshData(this);
            }
        } catch (Throwable ignored) {}
    }

    private void handle(StatusBarNotification sbn) {
        try {
            if (sbn == null) return;
            String pkg = sbn.getPackageName();
            if (pkg == null || pkg.equals(getPackageName())) return;
            Notification n = sbn.getNotification();
            if (n == null) return;

            Bitmap img = extractImage(n);
            if (img != null) ImageStore.putImage(this, pkg, img);

            // Roter Stern nur bei einer ECHTEN neuen Einzel-Benachrichtigung.
            if (Notifications.isReal(sbn)) {
                ImageStore.markUnread(this, pkg);
                ImageStore.clearDismissed(this, pkg); // wieder zeigen, es gibt Neues
            }

            FramesWidget.refreshData(this);
            // Nebenbei pruefen, ob die Bedienungshilfe (Kachel-Fotos) noch da
            // ist - falls ein OS-Update sie entzogen hat, hier erinnern (die App
            // muss dafuer nicht offen sein). Billig: nur einmal, bis wieder da.
            Perms.checkReminders(this);
        } catch (Throwable ignored) {}
    }

    /** Gibt es für dieses Paket noch eine aktive, echte Benachrichtigung?
     *  „Echt" = de.herbers.common.Notifications.isReal (Kategorie-Rauschen,
     *  Gruppen-Summary und nicht wegwischbare Meldungen ausgeschlossen). */
    private boolean hasRealNotification(String pkg) {
        try {
            StatusBarNotification[] active = getActiveNotifications();
            if (active == null) return false;
            for (StatusBarNotification s : active) {
                if (s != null && pkg.equals(s.getPackageName()) && Notifications.isReal(s)) return true;
            }
        } catch (Throwable ignored) {}
        return false;
    }

    private Bitmap extractImage(Notification n) {
        // Gemeinsame Bibliothek: deckt BigPictureStyle (Bitmap UND das
        // Icon-Format ab Android 12, EXTRA_PICTURE_ICON) sowie das grosse
        // Icon/Avatar ab - eine gepflegte Quelle statt einer eigenen Kopie.
        Bitmap pic = Notifications.bigPicture(this, n);
        if (pic != null) return pic;
        return Notifications.largeIcon(this, n);
    }
}
