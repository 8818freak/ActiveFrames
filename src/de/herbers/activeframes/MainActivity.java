package de.herbers.activeframes;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.text.TextUtils;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
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
        button(root, getString(R.string.backup_export), this::startBackupExport);
        button(root, getString(R.string.backup_import), this::startBackupImport);

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

        // Diagnose-Protokoll (gemeinsame Bibliothek) - bewusst ganz unten,
        // neueste Eintraege zuerst, Schalter/Knopf darueber, ein-/ausschaltbar.
        section(root, "Diagnose-Protokoll");
        body(root, "Zuletzt aufgezeichnete Fehler/Abstürze – hilft bei der Fehlersuche ohne Kabel. Neueste Einträge oben.");
        CheckBox showLog = new CheckBox(this);
        showLog.setText("  Protokoll anzeigen");
        showLog.setTextColor(Color.WHITE);
        showLog.setChecked(de.herbers.common.DiagLog.isDisplayEnabled(this));
        showLog.setOnCheckedChangeListener((v, on) -> {
            de.herbers.common.DiagLog.setDisplayEnabled(this, on); buildUi(); });
        root.addView(showLog);
        if (de.herbers.common.DiagLog.isDisplayEnabled(this)) {
            button(root, "Diagnose-Protokoll löschen",
                    () -> { de.herbers.common.DiagLog.clear(this); buildUi(); });
            String diag = de.herbers.common.DiagLog.readNewestFirst(this);
            TextView log = new TextView(this);
            log.setText((diag == null || diag.isEmpty()) ? "(noch leer)" : diag);
            log.setTextColor((diag == null || diag.isEmpty()) ? Color.GRAY : Color.parseColor("#CCCCCC"));
            log.setTextSize(11);
            log.setTypeface(android.graphics.Typeface.MONOSPACE);
            log.setTextIsSelectable(true);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = dp(6);
            log.setLayoutParams(lp);
            root.addView(log);
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

    // ---- Sichern/Wiederherstellen als echte Datei (Systemdialog, analog zu
    // EdgeTab) - erzeugt/liest eine .txt ueber das Storage Access Framework,
    // ohne Speicher-Berechtigung. ----
    private static final int REQ_BACKUP_EXPORT = 201;
    private static final int REQ_BACKUP_IMPORT = 202;

    /** Ordner, in dem der Nutzer seine Sicherungen sammelt - als Startordner
     *  vorschlagen (wird ignoriert, wenn der Picker es nicht unterstuetzt). */
    private static final Uri DASIS_FOLDER =
            Uri.parse("content://com.android.externalstorage.documents/document/primary%3ADaSis");

    private void startBackupExport() {
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_TITLE, "activeframes-sicherung.txt");
        i.putExtra(DocumentsContract.EXTRA_INITIAL_URI, DASIS_FOLDER);
        try { startActivityForResult(i, REQ_BACKUP_EXPORT); }
        catch (Exception e) { Toast.makeText(this, R.string.backup_bad, Toast.LENGTH_SHORT).show(); }
    }

    private void startBackupImport() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("text/plain");
        i.putExtra(DocumentsContract.EXTRA_INITIAL_URI, DASIS_FOLDER);
        try { startActivityForResult(i, REQ_BACKUP_IMPORT); }
        catch (Exception e) { Toast.makeText(this, R.string.backup_bad, Toast.LENGTH_SHORT).show(); }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == REQ_BACKUP_EXPORT) {
            if (res == RESULT_OK && data != null && data.getData() != null) {
                try (java.io.OutputStream out = getContentResolver().openOutputStream(data.getData())) {
                    out.write(Settings.exportText(this).getBytes("UTF-8"));
                    Toast.makeText(this, R.string.backup_saved, Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(this, R.string.backup_save_failed, Toast.LENGTH_SHORT).show();
                }
            }
        } else if (req == REQ_BACKUP_IMPORT) {
            if (res == RESULT_OK && data != null && data.getData() != null) {
                String text = readUri(data.getData());
                boolean ok = Settings.importText(this, text);
                Toast.makeText(this, getString(ok ? R.string.backup_restored : R.string.backup_bad),
                        Toast.LENGTH_LONG).show();
                if (ok) { FramesWidget.refreshData(this); buildUi(); }
            }
        }
    }

    private String readUri(Uri uri) {
        try (java.io.InputStream in = getContentResolver().openInputStream(uri)) {
            if (in == null) return null;
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
            return out.toString("UTF-8");
        } catch (Exception e) { return null; }
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
