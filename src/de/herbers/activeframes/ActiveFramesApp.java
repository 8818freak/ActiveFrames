package de.herbers.activeframes;

import android.app.Application;

import de.herbers.common.DiagLog;
import de.herbers.common.Diagnostics;

/**
 * App-Einstieg fuer alle Prozesse (Widget, Benachrichtigungs-Listener,
 * Bedienungshilfe-Dienst). Richtet die gemeinsame Diagnose ein: logcat-Tag
 * "ActiveFramesDiag" und einen Absturz-Logger, der unbehandelte Ausnahmen mit
 * vollem Stack ins Diagnose-Protokoll schreibt (in MainActivity einsehbar).
 */
public class ActiveFramesApp extends Application {
    @Override public void onCreate() {
        super.onCreate();
        DiagLog.setTag("ActiveFramesDiag");
        Diagnostics.installCrashLogger(this);
    }
}
