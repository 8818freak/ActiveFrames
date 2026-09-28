package de.herbers.activeframes;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.widget.RemoteViewsService;

/** Nur für den optionalen Scroll-Modus (Widget 1): verbindet das GridView mit
 *  dem Kachel-Nachschub. Standard ist die feste, korrekte Variante ohne
 *  Sammlung (siehe FramesWidget) - die Sammlung recycelt auf alten Launchern
 *  fehlerhaft, daher nur auf Wunsch mit Warnhinweis. */
public class FramesWidgetService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        int id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
        return new FramesRemoteViewsFactory(getApplicationContext(), id);
    }
}
