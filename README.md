# HRT Buddy

An Android reminder and tracking app for hormone replacement therapy (HRT).

HRT Buddy keeps the practical parts of an HRT routine in one place: patch changes, progesterone days, daily health check-ins, cycle history, and a quick home-screen widget.

## Features

- Configure cycle length and current cycle day
- Set patch days and a patch reminder time
- Set progesterone start/end days and reminder time
- Schedule a daily health check-in
- Optionally record:
  - Bleeding: none, spotting, light, medium, or heavy
  - Pain from 1–10
  - Mood from 1–10
  - Overall feeling from 1–10
- View the current cycle as a compact colour-coded map
- Review previous cycles in a multi-cycle summary
- Adjust the current cycle day with confirmation and data-safety warnings
- Import and export an Excel-compatible CSV backup
- Use temporary demo data for testing without replacing real data
- Add an Android home-screen widget with one-tap actions and cycle swimlanes

## Data and privacy

Data is stored locally on the device. The app does not require an account or a network connection for normal use.

Use the backup export before changing devices or clearing app data. The reset option clears stored schedules, history, check-ins, confirmations, skipped reminders, and demo data.

The app is a personal tracking and reminder tool, not a substitute for advice from a qualified clinician.

## Building

Requirements:

- Android Studio or a Java/Android build environment
- Android SDK 35
- JDK compatible with the Android Gradle Plugin used by the project

Build a debug APK with:

```bash
./gradlew testDebugUnitTest assembleDebug
```

The APK is produced at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Installing

Install the debug APK on an Android device or emulator. After installing, add the HRT Buddy widget from the Android home-screen widget picker. If a previous widget instance displays a loading error after an update, remove it and add it again so the launcher refreshes its cached layout.

## Project structure

- `app/src/main/java/com/hrtbuddy/` — app, reminder, persistence, backup, and widget logic
- `app/src/main/res/` — Android resources and widget layouts
- `app/src/test/` — unit tests

## Release

The current release is [v1.0.0](https://github.com/chrissyhroberts/HRT_Buddy/releases/tag/v1.0.0).
