# Active Frames

A from-scratch Android recreation of BlackBerry OS10's "Active Frames" — a
resizable home-screen widget of your most-recently-used apps as a scrollable
grid of tiles, each showing the content of the app's latest notification
(photo, album art, large icon) with a red star when there's something new.

*Eine von Grund auf neu geschriebene Nachbildung von BlackBerry OS10s „Active
Frames" — ein größenveränderbares Startbildschirm-Widget mit den zuletzt
benutzten Apps als scrollbares Kachelraster; jede Kachel zeigt den Inhalt der
letzten Benachrichtigung, roter Stern bei Neuem.*

## Features

- **Scrollable tile grid widget**, freely resizable. Adjustable columns, tile
  height (i.e. aspect ratio — rectangular like BlackBerry's screens, not only
  square), tile count (scroll up for the rest), and labels on/off.
- **MRU order like BlackBerry**: top-left is the most recently used app, then
  the one before, and so on (via `UsageStatsManager`).
- **Living tiles**: each tile shows the latest notification's image — chat
  photo, album art, large icon — falling back to the app icon
  (`NotificationListenerService`).
- **Red star** on a tile when that app has something new; cleared on launch.
- No ads, no analytics, no billing, no internet permission.

## The honest limitation

True per-app screen snapshots (BlackBerry's actual live frames) are **not
available to a non-system app** on Android — only the system launcher, bound
to SystemUI over privileged interfaces, can read `TaskSnapshot`s, and no
launcher exposes them to other apps. The notification image is the closest
feasible substitute (and is what commercial equivalents use, too).

## Building

No Gradle — built directly with the raw Android SDK command-line tools. You
need `platforms;android-34`, `build-tools;34.0.0`, `platform-tools`, a JDK
(11+), and your own signing keystore (none is included).

```sh
SDK=/path/to/android/sdk
BT="$SDK/build-tools/34.0.0"
AJAR="$SDK/platforms/android-34/android.jar"

rm -rf build && mkdir -p build/gen build/obj
"$BT/aapt2" compile --dir res -o build/res.zip
"$BT/aapt2" link -o build/base.apk -I "$AJAR" --manifest AndroidManifest.xml \
  --java build/gen -R build/res.zip -A assets --auto-add-overlay \
  --min-sdk-version 29 --target-sdk-version 34
javac --release 11 -d build/obj -cp "$AJAR" $(find src build/gen -name '*.java')
"$BT/d8" --min-api 29 --lib "$AJAR" --output build/ $(find build/obj -name '*.class')
cp build/base.apk build/unsigned.apk && (cd build && zip -qj unsigned.apk classes*.dex)
"$BT/zipalign" -f -p 4 build/unsigned.apk build/aligned.apk
"$BT/apksigner" sign --ks keystore.p12 --ks-pass pass:YOURPASS --ks-key-alias YOURALIAS \
  --out build/ActiveFrames.apk build/aligned.apk
```

## License

GNU General Public License v3.0 (or later) — see `LICENSE`.
