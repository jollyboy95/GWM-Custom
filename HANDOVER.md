# GWM Custom handover

## Where to resume

v0.1.0 is a Kotlin and Jetpack Compose Android preview. The Garage has five demo profiles (Haval Jolion, H7, H6, Tank 300 and Tank 500), simulated HUT and CEM settings, and an Expert mode toggle. History can save and restore local profile snapshots and shows the latest 200 events per profile. Bluetooth performs a foreground BLE scan and suggests ThinkDiag matches by advertised name.

Use [the repository](https://github.com/jollyboy95/GWM-Custom) as the source of truth. Start from the latest `main`, inspect Git status and recent commits, then check the [Android APK workflow](https://github.com/jollyboy95/GWM-Custom/actions/workflows/android.yml) for the commit you are resuming from.

## Code map

- `MainActivity.kt` contains the Garage, Bluetooth and History screens and the runtime permission flow.
- `Demo.kt` holds the demo vehicle and setting catalog, snapshots and history rules.
- `DemoStore.kt` stores each profile’s configuration, snapshots and history together in app-private preferences.
- `BleDiscovery.kt` handles scan status, timeout, deduplication and name-based hints. Scan results stay in memory and are cleared when leaving Bluetooth or backgrounding the app.
- `.github/workflows/android.yml` runs the tests and lint and uploads the debug APK.

The Kotlin sources are in `app/src/main/java/com/gwmcustom/app/`.

## Build and verification

Use JDK 17, Android SDK Platform 35 and Build Tools 35.0.0.

```sh
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

Local unit tests, lint and debug assembly passed for v0.1.0. The tests cover unchanged settings, restoring settings from both modules, history and rejecting a snapshot absent from the profile. The local APK is at `app/build/outputs/apk/debug/app-debug.apk`. For a GitHub build, open the successful run and download `gwm-custom-0.1.0-debug` under **Artifacts**.

## Next verification pass

1. Install the APK on an Android phone. In Garage, edit a setting, enable Expert mode and switch between two profiles to check that each keeps its own settings.
2. Save a backup, change settings in both modules, then restore. Restart the app and check the restored values and history. Check the layout in light and dark mode.
3. On Android 12 or newer, test nearby-device permission denial and grant, scan cancellation and scan timeout. On Android 8–11, check location permission and the Location-off message. An actual ThinkDiag adapter is needed to verify its advertised name and discovery in the field. Device UI and live adapter behavior still need this pass.

The v0.1.0 settings are fictional and shared across the five profiles. Keep this public repository free of proprietary ThinkDiag APKs, libraries, databases, licensing files and captured device identifiers. The app’s hardware scope is BLE discovery. Pairing, diagnostics, protocol communication and vehicle writes are outside this preview.
