# GWM Custom

GWM Custom v0.1.0 is a native Android preview built with Kotlin and Jetpack Compose. It runs on Android 8.0 and newer and follows the phone’s light or dark theme.

- **Garage** — Select Haval Jolion, H7, H6, Tank 300 or Tank 500. Try simulated HUT (head unit) and CEM (central electronics) settings. Enable Expert mode to reveal additional examples.
- **Bluetooth** — Run a 12-second foreground Bluetooth LE scan. Possible ThinkDiag adapters are highlighted when their advertised name includes ThinkDiag. Android 12+ requests Nearby devices access. Older Android versions require location permission and Location enabled.
- **History** — Save and restore a local snapshot of the selected demo profile and review its latest 200 events. Settings, backups and history are stored separately for each profile on the phone.

The settings are fictional examples shared across the five demo profiles. The app has no pairing, adapter connection, vehicle communication or vehicle write implementation. The repository contains original app code and no proprietary ThinkDiag files or mappings.

## Build and install

Use JDK 17 and Android SDK Platform 35 with Build Tools 35.0.0. Open the project in Android Studio or run:

```sh
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`. The [Android APK workflow](https://github.com/jollyboy95/GWM-Custom/actions/workflows/android.yml) runs the same checks on pushes, pull requests and manual runs. Download the debug APK from a successful run’s **Artifacts** section.
