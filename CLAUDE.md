# WRK

Personal Android deep-work timer (app name **WRK**, package still `com.geomar.focuslock`). During a session the app pins itself fullscreen (Android screen pinning / `startLockTask`) so the phone is unusable until the countdown ends. Single user (Geomar), not published. Keep it simple — no libraries beyond AndroidX/Compose unless clearly needed.

## UI conventions

Professional, minimal, pure black-and-white. Rules live in [ui/Theme.kt](app/src/main/java/com/geomar/focuslock/ui/Theme.kt):

- Pure black background, white text; only alpha steps for hierarchy (`WrkDim` 35%, `WrkFaint` 15%). No color, no elevation, no rounded corners (`RectangleShape`).
- Big numerals: `FontWeight.ExtraLight`, 96–128sp. Labels: `WrkCaption` — uppercase, 12sp, 4sp letterspacing.
- Shared components: `WrkButton` (solid white primary), `WrkGhostButton` (hairline outline), `WrkCaption`. Reuse them; don't inline one-off buttons.
- Logo/wordmark: "WRK" letterspaced; launcher icon = white W on near-black (adaptive vector).

## Stack

- Kotlin + Jetpack Compose (Material 3), single `:app` module
- minSdk 26, targetSdk/compileSdk 35, JDK 17
- No nav library — screens switch on `TimerState` (Idle / Running / Finished)
- No DI, no network, no database. Session state persists in SharedPreferences (`end_time`, `total`) so a session survives process death.

## Architecture

- [TimerViewModel.kt](app/src/main/java/com/geomar/focuslock/TimerViewModel.kt) — all timer logic. Wall-clock based: remaining time is always `endTime - System.currentTimeMillis()`, never accumulated ticks (immune to doze/screen-off drift). Recovers running session in `init`. Also owns user task templates (name + duration, `templates` key) and completed work days (`work_days` key, ISO dates), both persisted as JSON in the same prefs. A session that expires while the process is dead still records its day on next launch.
- [MainActivity.kt](app/src/main/java/com/geomar/focuslock/MainActivity.kt) — reacts to state: `startLockTask()`/`stopLockTask()`, `FLAG_KEEP_SCREEN_ON`, back-press swallow, completion vibrate + looping system alarm (`USAGE_ALARM`, stops on Done). Polls `ActivityManager.lockTaskModeState` every 500ms while running: if user escapes pinning mid-session, SessionScreen shows "Resume focus" (re-pin) and "End session" (two-tap confirm, cancels timer). Those controls never render while pinned.
- [ui/SetupScreen.kt](app/src/main/java/com/geomar/focuslock/ui/SetupScreen.kt), [ui/SessionScreen.kt](app/src/main/java/com/geomar/focuslock/ui/SessionScreen.kt) — dumb composables.
- [ui/HistoryScreen.kt](app/src/main/java/com/geomar/focuslock/ui/HistoryScreen.kt) — streak numeral + Monday-first month calendar of work days; `currentStreak()` counts consecutive days ending today (or yesterday if today not done yet). Opened from a caption on SetupScreen via a local `showHistory` flag in MainActivity.

## Build & install

```bash
# JDK 17 is a homebrew keg (not in /Library/Java, so java_home can't find it)
export JAVA_HOME=$(brew --prefix openjdk@17)/libexec/openjdk.jdk/Contents/Home
./gradlew assembleDebug          # build APK
./gradlew installDebug           # build + install on USB-connected phone
adb devices                      # verify phone connected
```

SDK lives at `~/Library/Android/sdk` (installed via `android-commandlinetools` brew cask, not Android Studio). `local.properties` points to it.

## Screen pinning caveats

- Phone must have App pinning enabled: Settings → Security → App pinning (name varies by OEM).
- First `startLockTask()` shows a system confirmation dialog.
- Escape is possible: hold Back + Recents ~3s. This is intentional friction, not a hard lock.
- Upgrade path to true kiosk: Device Owner via `adb shell dpm set-device-owner` — see plan notes, not implemented.

## Testing flow

No emulator setup. Test on physical phone via USB: `./gradlew installDebug`, run 1-minute session, verify pin, countdown, unlock + vibrate at zero. Edge cases that matter: rotation mid-session, screen off/on mid-session, force-kill mid-session (should recover from SharedPreferences).

## Ideas backlog (not built)

Device Owner kiosk mode, richer session stats (durations, totals), foreground service + notification, scheduled sessions, strict-mode toggle.
