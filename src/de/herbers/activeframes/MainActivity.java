package de.herbers.activeframes;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/** Startbildschirm: erklaert Active Frames und fuehrt durch die zwei
 *  Freischaltungen (Nutzungsdatenzugriff fuer die Reihenfolge, Benachrichti-
 *  gungszugriff fuer Stern + Kachelbild). */
public class MainActivity extends Activity {

    private TextView usageStatus, notifStatus, accStatus;
    private boolean changelogOpen = false;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        buildUi();
    }

    private void buildUi() {
        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(Color.parseColor("#101216"));
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(24));
        sv.addView(root);

        title(root, getString(R.string.app_name));
        body(root, getString(R.string.main_intro));

        section(root, getString(R.string.sec_order));
        body(root, getString(R.string.order_desc));
        usageStatus = statusLine(root);
        button(root, getString(R.string.grant_usage), () ->
                openSettings(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS));

        section(root, getString(R.string.sec_star));
        body(root, getString(R.string.star_desc));
        notifStatus = statusLine(root);
        button(root, getString(R.string.grant_notif), () ->
                openSettings(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));

        section(root, getString(R.string.sec_photos));
        body(root, getString(R.string.photos_desc));
        accStatus = statusLine(root);
        button(root, getString(R.string.grant_acc), () ->
                openSettings(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS));

        section(root, getString(R.string.sec_widget));
        body(root, getString(R.string.main_widget_desc));
        button(root, getString(R.string.open_settings_btn), () ->
                startActivity(new Intent(this, ConfigActivity.class)));

        section(root, getString(R.string.sec_backup));
        body(root, getString(R.string.backup_desc));
        button(root, getString(R.string.backup_export), this::showExport);
        button(root, getString(R.string.backup_import), this::showImport);

        section(root, getString(R.string.changelog_title));
        button(root, getString(changelogOpen ? R.string.changelog_hide : R.string.changelog_show),
                () -> { changelogOpen = !changelogOpen; buildUi(); });
        if (changelogOpen) {
            String md = readAsset("CHANGELOG.md");
            if (md == null) {
                body(root, getString(R.string.changelog_unavailable));
            } else {
                renderMarkdown(root, md);
            }
        }

        setContentView(sv);
        updateStatuses();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatuses();
    }

    private void updateStatuses() {
        if (usageStatus == null) return;
        boolean usage = UsageProvider.hasAccess(this);
        usageStatus.setText(getString(usage ? R.string.status_granted : R.string.status_denied));
        usageStatus.setTextColor(usage ? Color.parseColor("#5BD68A") : Color.parseColor("#E0533A"));
        boolean notif = notifAccess(this);
        notifStatus.setText(getString(notif ? R.string.status_granted : R.string.status_denied));
        notifStatus.setTextColor(notif ? Color.parseColor("#5BD68A") : Color.parseColor("#E0533A"));
        boolean acc = accAccess(this);
        accStatus.setText(getString(acc ? R.string.acc_on : R.string.acc_off));
        accStatus.setTextColor(acc ? Color.parseColor("#5BD68A") : Color.parseColor("#99AAB8"));
        FramesWidget.refreshData(this);
    }

    static boolean notifAccess(Context c) {
        String flat = android.provider.Settings.Secure.getString(c.getContentResolver(),
                "enabled_notification_listeners");
        if (TextUtils.isEmpty(flat)) return false;
        ComponentName me = new ComponentName(c, FramesNotificationListener.class);
        for (String entry : flat.split(":")) {
            ComponentName cn = ComponentName.unflattenFromString(entry);
            if (cn != null && cn.equals(me)) return true;
        }
        return false;
    }

    static boolean accAccess(Context c) {
        String flat = android.provider.Settings.Secure.getString(c.getContentResolver(),
                "enabled_accessibility_services");
        if (TextUtils.isEmpty(flat)) return false;
        ComponentName me = new ComponentName(c, FramesAccessibilityService.class);
        for (String entry : flat.split(":")) {
            ComponentName cn = ComponentName.unflattenFromString(entry);
            if (cn != null && cn.equals(me)) return true;
        }
        return false;
    }

    // ---- Sichern/Wiederherstellen (analog zu EdgeTab) ----
    private void showExport() {
        final String text = Settings.exportText(this);
        EditText ed = new EditText(this);
        ed.setText(text);
        ed.setTextSize(11);
        ed.setTextColor(Color.WHITE);
        ed.setKeyListener(null); // schreibgeschuetzt, aber markier-/kopierbar
        new AlertDialog.Builder(this)
                .setTitle(R.string.backup_export)
                .setView(wrapInScroll(ed))
                .setPositiveButton(R.string.backup_copy, (d, w) -> {
                    ClipboardManager cb = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    if (cb != null) cb.setPrimaryClip(ClipData.newPlainText("Active Frames Backup", text));
                    Toast.makeText(this, R.string.backup_copied, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showImport() {
        EditText ed = new EditText(this);
        ed.setHint(R.string.backup_paste_hint);
        ed.setTextSize(11);
        ed.setTextColor(Color.WHITE);
        new AlertDialog.Builder(this)
                .setTitle(R.string.backup_import)
                .setView(wrapInScroll(ed))
                .setPositiveButton(R.string.backup_restore_btn, (d, w) -> {
                    boolean ok = Settings.importText(this, ed.getText().toString().trim());
                    Toast.makeText(this, getString(ok ? R.string.backup_restored : R.string.backup_bad),
                            Toast.LENGTH_LONG).show();
                    if (ok) { FramesWidget.refreshData(this); buildUi(); }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private ScrollView wrapInScroll(View v) {
        ScrollView s = new ScrollView(this);
        int pad = dp(16);
        s.setPadding(pad, dp(8), pad, dp(8));
        s.addView(v);
        return s;
    }

    private void openSettings(String action) {
        try { startActivity(new Intent(action)); }
        catch (Throwable t) {
            try { startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS)); } catch (Throwable ignored) {}
        }
    }

    // ---- UI-Helfer ----
    private void title(LinearLayout root, String s) {
        TextView t = new TextView(this); t.setText(s);
        t.setTextColor(Color.parseColor("#2E9BE6")); t.setTextSize(24);
        root.addView(t);
    }
    private void section(LinearLayout root, String s) {
        TextView t = new TextView(this); t.setText(s);
        t.setTextColor(Color.parseColor("#2E9BE6")); t.setTextSize(15);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(18); t.setLayoutParams(lp);
        root.addView(t);
    }
    private void body(LinearLayout root, String s) {
        TextView t = new TextView(this); t.setText(s);
        t.setTextColor(Color.parseColor("#CCCCCC")); t.setTextSize(13);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(4); t.setLayoutParams(lp);
        root.addView(t);
    }
    private TextView statusLine(LinearLayout root) {
        TextView t = new TextView(this); t.setTextSize(13);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(4); t.setLayoutParams(lp);
        root.addView(t); return t;
    }
    private void button(LinearLayout root, String s, Runnable onClick) {
        Button btn = new Button(this); btn.setText(s);
        btn.setOnClickListener(v -> onClick.run());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(6); btn.setLayoutParams(lp);
        root.addView(btn);
    }
    private int dp(float v) { return (int) (v * getResources().getDisplayMetrics().density); }

    private String readAsset(String name) {
        try (java.io.InputStream in = getAssets().open(name)) {
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[4096]; int n;
            while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
            return out.toString("UTF-8");
        } catch (Exception e) { return null; }
    }

    /** Sehr schlichter Markdown-Darsteller - nur genug fuers eigene, immer
     *  gleich aufgebaute CHANGELOG.md (Ueberschriften, Aufzaehlungen, Absaetze). */
    private void renderMarkdown(LinearLayout root, String md) {
        for (String line : md.split("\n")) {
            String t = line.trim();
            if (t.isEmpty()) continue;
            TextView tv = new TextView(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            if (t.startsWith("## ")) {
                tv.setText(t.substring(3));
                tv.setTextColor(Color.parseColor("#2E9BE6"));
                tv.setTextSize(15);
                lp.topMargin = dp(12);
            } else if (t.startsWith("# ")) {
                tv.setText(t.substring(2));
                tv.setTextColor(Color.parseColor("#2E9BE6"));
                tv.setTextSize(18);
                lp.topMargin = dp(10);
            } else if (t.startsWith("- ")) {
                tv.setText("•  " + t.substring(2));
                tv.setTextColor(Color.parseColor("#CCCCCC"));
                tv.setTextSize(13);
                lp.topMargin = dp(3);
            } else {
                tv.setText(t);
                tv.setTextColor(Color.parseColor("#9AA6B2"));
                tv.setTextSize(13);
                lp.topMargin = dp(4);
            }
            tv.setLayoutParams(lp);
            root.addView(tv);
        }
    }
}
