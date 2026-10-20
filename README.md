# Mobile Data Tile

An Android Quick Settings tile that shows how much mobile (cellular) data
you've used, right in the pull-down drawer next to your WiFi / hotspot /
Bluetooth toggles.

- **Daily mode (default):** counts usage from midnight to now.
- **Billing cycle mode:** counts from a configurable reset day (1–31) of
  each month.
- Tap the tile to open the app; the app just switches modes and handles
  the one-time permission.

## Install

1. Grab the APK from the [latest release](../../releases/latest) (or build
   it yourself, see below).
2. Install it on your phone (open the APK and allow "install unknown apps"
   if prompted).
3. Open **Mobile Data Tile** and tap **Grant usage access**, then enable
   this app in the *Usage access* list.
4. Pull the notification shade all the way down, tap the edit (pencil)
   icon, and drag **Mobile data** into your active tiles.

The tile works on Android 8.0+ (API 26). No internet permission, no
tracking, no accounts — the app only reads the OS's own network stats.

## How it works

Android keeps per-app network usage in `NetworkStatsManager`, the same
data used by *Settings → Network → Data usage*. This app reads it with
the `PACKAGE_USAGE_STATS` special permission (a one-time toggle in
Settings) and renders it in a `TileService`.

The tile refreshes:

- every time you open the Quick Settings panel, and
- once a day just after midnight (via an inexact alarm, which survives
  reboots through a `BOOT_COMPLETED` receiver).

Dual-SIM devices report the combined total of all subscriptions.

## Build

Requires JDK 17 and the Android SDK (platform 35).

```sh
./gradlew assembleRelease
# APK lands in app/build/outputs/apk/release/
```

For a signed release, create a keystore and point Gradle at it:

```sh
keytool -genkey -v -keystore release.jks -keyalg RSA \
        -keysize 2048 -validity 10000 -alias release

cat > keystore.properties <<EOF
KEYSTORE_FILE=release.jks
KEYSTORE_PASSWORD=<password>
KEY_ALIAS=release
KEY_PASSWORD=<password>
EOF
```

## License

[MIT](LICENSE)
