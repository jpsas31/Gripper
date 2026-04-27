# WH-C06 Trainer

Personal Android POC. Reads Weiheng WH-C06 hanging scale via BLE advertisement, runs finger-strength training programs (MVC, Critical Force, Repeaters, Max Force, Endurance, etc.).

## Setup

1. Open this folder (`~/wh-c06-android/app`) in Android Studio (Hedgehog or newer).
2. Let Gradle sync — first run downloads ~500MB.
3. Connect Android phone via USB (Developer Mode + USB debugging on).
4. Run.

## Build from CLI

```bash
cd ~/wh-c06-android/app
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

(`gradlew` wrapper jar created on first sync inside Android Studio.)

## Permissions

App requests at launch:
- **Android 12+**: `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`.
- **Android 11 and below**: `BLUETOOTH`, `BLUETOOTH_ADMIN`, `ACCESS_FINE_LOCATION`.

Min SDK = 21 (Android 5.0). Tested target = API 35.

## Architecture

```
ble/
  WhC06Parser.kt   — manufacturer-data byte parser (offset 10, big-endian uint16, /100)
  BleScanner.kt    — BluetoothLeScanner, low-latency, aggressive match
  Smoother.kt      — EMA + windowed peak tracker
training/
  Programs.kt      — Program library (assessments + training plans)
  SessionEngine.kt — phase-by-phase runtime
ui/
  MainViewModel.kt — StateFlow facade for screens
  ForceGauge.kt    — animated half-arc gauge (Compose Canvas)
  ForceChart.kt    — rolling 30s force-time chart
  Screens.kt       — Live / Programs / Settings tabs
  theme/Theme.kt   — Material3 dark/light
MainActivity.kt    — single-activity Compose host
```

## Programs included

**Assessments**: MVC, Peak Force, Critical Force (24 reps × 7s/3s), Endurance.
**Training**: Warmup, Max Force, Repeaters, Endurance, No-Hang Max, Active Recovery.

## Limitations

- Sample rate ~5-10 Hz on Android (WH-C06 BLE advertisement interval).
- RFD assessments removed — sample rate too low.
- Manufacturer ID 256 = TomTom-squatted, may false-trigger on rare TomTom devices.
- No persistence yet — sessions lost on app close. Add Room DB later.

## References

- Stevie-Ray/hangtime-grip-connect — WH-C06 protocol (TS).
- sebws/Crane — iOS reference implementation.
- Frez (Flutter, closed-source) — program structure inspiration.
