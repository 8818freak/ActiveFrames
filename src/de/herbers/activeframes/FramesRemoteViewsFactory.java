package de.herbers.activeframes;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import java.util.ArrayList;
import java.util.List;

/** Kachel-Nachschub für den optionalen Scroll-Modus (GridView-Sammlung).
 *  ACHTUNG: Auf alten Launchern recycelt die Sammlung Kacheln fehlerhaft
 *  (Bild/Name können verrutschen) - deshalb ist der Scroll-Modus optional und
 *  mit Warnhinweis versehen; die feste Variante (FramesWidget.updateWidget)
 *  ist immer korrekt. */
class FramesRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {

    private final Context ctx;
    private final int appWidgetId;
    private volatile List<String> pkgs = new ArrayList<>();

    FramesRemoteViewsFactory(Context ctx, int appWidgetId) {
        this.ctx = ctx;
        this.appWidgetId = appWidgetId;
    }

    @Override public void onCreate() {}
    @Override public void onDestroy() { pkgs = new ArrayList<>(); }
    @Override public void onDataSetChanged() { pkgs = FramesWidget.orderedPkgs(ctx); }

    @Override public int getCount() { return pkgs.size(); }
    @Override public int getViewTypeCount() { return 1; }
    @Override public long getItemId(int position) {
        List<String> l = pkgs;
        return (position >= 0 && position < l.size()) ? l.get(position).hashCode() : position;
    }
    @Override public boolean hasStableIds() { return true; }
    @Override public RemoteViews getLoadingView() { return null; }

    @Override public RemoteViews getViewAt(int position) {
        RemoteViews t = new RemoteViews(ctx.getPackageName(), R.layout.tile_scroll);
        List<String> l = pkgs;
        if (position < 0 || position >= l.size()) return t;
        String pkg = l.get(position);

        if (Build.VERSION.SDK_INT >= 31) {
            int h = FramesWidget.tileHeightDp(ctx, AppWidgetManager.getInstance(ctx), appWidgetId);
            t.setViewLayoutHeight(R.id.tile_root, h, TypedValue.COMPLEX_UNIT_DIP);
        }

        String label = AppInfoCache.label(ctx, pkg);
        Bitmap small = AppInfoCache.icon(ctx, pkg, 40);
        if (small != null) t.setImageViewBitmap(R.id.tile_icon, small);
        t.setTextViewText(R.id.tile_label, label);

        if (ImageStore.hasImage(ctx, pkg)) {
            Uri uri = ImageStore.uriFor(ctx, pkg);
            String lp = FramesWidget.launcherPkg(ctx);
            if (lp != null) {
                try { ctx.grantUriPermission(lp, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION); }
                catch (Throwable ignored) {}
            }
            t.setImageViewUri(R.id.tile_image, uri);
            t.setViewVisibility(R.id.tile_image, View.VISIBLE);
            t.setViewVisibility(R.id.tile_title, View.GONE);
        } else {
            t.setViewVisibility(R.id.tile_image, View.GONE);
            t.setTextViewText(R.id.tile_title, label);
            t.setViewVisibility(R.id.tile_title, View.VISIBLE);
        }
        t.setViewVisibility(R.id.tile_star, ImageStore.isUnread(ctx, pkg) ? View.VISIBLE : View.GONE);

        Intent open = new Intent().setData(Uri.parse("af://open/" + pkg))
                .putExtra(FramesWidget.EXTRA_PKG, pkg);
        t.setOnClickFillInIntent(R.id.tile_card, open);
        Intent dismiss = new Intent().setData(Uri.parse("af://dismiss/" + pkg))
                .putExtra(FramesWidget.EXTRA_PKG, pkg).putExtra(FramesWidget.EXTRA_DISMISS, true);
        t.setOnClickFillInIntent(R.id.tile_close, dismiss);
        return t;
    }
}
