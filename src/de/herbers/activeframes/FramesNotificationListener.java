package de.herbers.activeframes;

import android.app.Notification;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

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
            if (isReal(sbn)) {
                ImageStore.markUnread(this, pkg);
                ImageStore.clearDismissed(this, pkg); // wieder zeigen, es gibt Neues
            }

            FramesWidget.refreshData(this);
        } catch (Throwable ignored) {}
    }

    /** Eine „echte“, den Stern rechtfertigende Einzel-Benachrichtigung.
     *  Ausgeschlossen (sonst falsche Sterne, z. B. beim Hub-Posteingang):
     *  dauerhafte/nicht wegwischbare Meldungen (laufende Dienste), Gruppen-
     *  Sammelmeldungen (FLAG_GROUP_SUMMARY) und Dienst-/Transport-/Fortschritts-/
     *  System-Kategorien (Sync-Hinweise, Mediensteuerung, Downloads …). */
    private static boolean isReal(StatusBarNotification sbn) {
        Notification n = sbn.getNotification();
        if (n == null) return false;
        String cat = n.category;
        boolean noise = Notification.CATEGORY_SERVICE.equals(cat)
                || Notification.CATEGORY_TRANSPORT.equals(cat)
                || Notification.CATEGORY_PROGRESS.equals(cat)
                || Notification.CATEGORY_SYSTEM.equals(cat);
        boolean summary = (n.flags & Notification.FLAG_GROUP_SUMMARY) != 0;
        return sbn.isClearable() && !summary && !noise;
    }

    /** Gibt es für dieses Paket noch eine aktive, echte Benachrichtigung? */
    private boolean hasRealNotification(String pkg) {
        try {
            StatusBarNotification[] active = getActiveNotifications();
            if (active == null) return false;
            for (StatusBarNotification s : active) {
                if (s != null && pkg.equals(s.getPackageName()) && isReal(s)) return true;
            }
        } catch (Throwable ignored) {}
        return false;
    }

    private Bitmap extractImage(Notification n) {
        try {
            Bundle x = n.extras;
            if (x != null) {
                Object pic = x.getParcelable(Notification.EXTRA_PICTURE); // BigPictureStyle
                if (pic instanceof Bitmap) return (Bitmap) pic;
            }
            Icon large = n.getLargeIcon(); // Kontaktfoto / Albumcover / grosses Symbol
            if (large != null) {
                Drawable d = large.loadDrawable(this);
                Bitmap b = AppInfoCache.toBitmap(d, 0);
                if (b != null) return b;
            }
        } catch (Throwable ignored) {}
        return null;
    }
}
