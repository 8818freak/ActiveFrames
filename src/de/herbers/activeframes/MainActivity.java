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

    private boolean changelogOpen = false;
    private Boolean permsOpen = null; // null = noch nicht entschieden (Auto: offen, wenn etwas fehlt)

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

        // Berechtigungen: EIN erklaerter, aufklappbarer Abschnitt (keine
        // doppelten Einstellungen mehr - ersetzt die frueheren Einzelabschnitte
        // Reihenfolge/Stern/Fotos). Erklaerung, Status und Sprung in die
        // Systemeinstellung stehen je Berechtigung beisammen.
        permsSection(root);

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
        // Ab Android 13 braucht das Posten von Benachrichtigungen eine
        // Laufzeit-Berechtigung - fuer die Bedienungshilfe-Erinnerung einmal
        // anfragen. Danach pruefen und ggf. erinnern/zurueckziehen.
        if (android.os.Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                        != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            try { requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 909); }
            catch (Throwable ignored) {}
        }
        Perms.checkReminders(this);
    }

    private void updateStatuses() {
        // Der Berechtigungs-Status steht jetzt im aufklappbaren Abschnitt
        // (permsSection); hier nur noch das Widget auffrischen.
        FramesWidget.refreshData(this);
    }

    /** Aufklappbarer Berechtigungs-Abschnitt (Dreieck ▸/▾): listet alle
     *  Berechtigungen dieser App mit Status; ein Tipp auf eine Zeile fuehrt in
     *  die passende Systemeinstellung. Dieselbe Definition wie die Erinnerung
     *  (Perms.list). */
    private void permsSection(LinearLayout root) {
        java.util.List<de.herbers.common.PermReminder.Perm> perms = Perms.list(this);
        boolean anyMissing = false;
        for (de.herbers.common.PermReminder.Perm perm : perms) if (!perm.granted) anyMissing = true;
        // Auto: offen, solange etwas fehlt (Einrichtung sichtbar); sonst zu.
        boolean open = (permsOpen != null) ? permsOpen : anyMissing;

        TextView head = new TextView(this);
        head.setText((open ? "▾ " : "▸ ") + "Berechtigungen");
        head.setTextColor(Color.parseColor("#2E9BE6"));
        head.setTextSize(16);
        head.setPadding(0, dp(16), 0, dp(8));
        final boolean cur = open;
        head.setOnClickListener(v -> { permsOpen = !cur; buildUi(); });
        root.addView(head);
        if (!open) return;

        for (de.herbers.common.PermReminder.Perm perm : perms) {
            TextView name = new TextView(this);
            name.setText((perm.granted ? "✓  " : "✗  ") + perm.label
                    + (perm.granted ? "" : "  –  fehlt"));
            name.setTextColor(perm.granted ? Color.parseColor("#5BD68A") : Color.parseColor("#E0533A"));
            name.setTextSize(15);
            name.setPadding(dp(4), dp(10), dp(4), dp(2));
            root.addView(name);

            if (perm.explanation != null && !perm.explanation.isEmpty()) {
                TextView why = new TextView(this);
                why.setText(perm.explanation);
                why.setTextColor(Color.parseColor("#99AAB8"));
                why.setTextSize(12.5f);
                why.setPadding(dp(4), 0, dp(4), dp(4));
                root.addView(why);
            }

            Button go = new Button(this);
            go.setText(perm.granted ? "In den Einstellungen ändern" : "Jetzt erteilen");
            go.setOnClickListener(v -> {
                try {
                    Intent i = perm.settings;
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                } catch (Throwable t) {
                    Toast.makeText(this, "Einstellung nicht verfügbar", Toast.LENGTH_SHORT).show();
                }
            });
            root.addView(go);
        }

        // Zugehoerige, gespeicherte Daten: die Kachelbilder (aus Benachrichtigungen
        // und Bildschirmfotos). Gezielt loeschbar, mit Sicherheitsabfrage.
        TextView clearHint = new TextView(this);
        clearHint.setText("Gespeicherte Kachelbilder (aus Benachrichtigungen und Bildschirmfotos):");
        clearHint.setTextColor(Color.parseColor("#8899AA"));
        clearHint.setTextSize(12);
        clearHint.setPadding(dp(4), dp(12), dp(4), dp(2));
        root.addView(clearHint);
        Button delImgs = new Button(this);
        delImgs.setText("Kachelbilder löschen");
        delImgs.setTextColor(Color.parseColor("#E06666"));
        delImgs.setOnClickListener(v -> confirmClear("Kachelbilder löschen", () -> {
            ImageStore.clearAllImages(this);
            FramesWidget.rebuildAll(this);
        }));
        root.addView(delImgs);
    }

    /** Sicherheitsabfrage vor dem Loeschen. */
    private void confirmClear(String what, Runnable action) {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Wirklich löschen?")
                .setMessage("„" + what + "“ – das lässt sich nicht rückgängig machen. "
                        + "Die Kacheln bauen sich beim nächsten Öffnen/bei neuen Benachrichtigungen neu auf.")
                .setNegativeButton("Abbrechen", null)
                .setPositiveButton("Löschen", (dlg, w) -> {
                    try { action.run(); Toast.makeText(this, "Gelöscht.", Toast.LENGTH_SHORT).show(); }
                    catch (Throwable t) {}
                    buildUi();
                })
                .show();
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
