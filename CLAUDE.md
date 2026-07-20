# FocusLock

Personal Android deep-work timer. During a session the app pins itself fullscreen (Android screen pinning / `startLockTask`) so the phone is unusable until the countdown ends. Single user (Geomar), not published. Keep it simple — no libraries beyond AndroidX/Compose unless clearly needed.

## Stack

- Kotlin + Jetpack Compose (Material 3), single `:app` module
- minSdk 26, targetSdk/compileSdk 35, JDK 17
- No nav library — screens switch on `TimerState` (Idle / Running / Finished)
- No DI, no network, no database. Session state persists in SharedPreferences (`end_time`, `total`) so a session survives process death.

## Architecture

- [TimerViewModel.kt](app/src/main/java/com/geomar/focuslock/TimerViewModel.kt) — all timer logic. Wall-clock based: remaining time is always `endTime - System.currentTimeMillis()`, never accumulated ticks (immune to doze/screen-off drift). Recovers running session in `init`.
- [MainActivity.kt](app/src/main/java/com/geomar/focuslock/MainActivity.kt) — reacts to state: `startLockTask()`/`stopLockTask()`, `FLAG_KEEP_SCREEN_ON`, back-press swallow, completion vibrate+sound.
- [ui/SetupScreen.kt](app/src/main/java/com/geomar/focuslock/ui/SetupScreen.kt), [ui/SessionScreen.kt](app/src/main/java/com/geomar/focuslock/ui/SessionScreen.kt) — dumb composables.

## Build & install

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
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

Device Owner kiosk mode, session history/stats, foreground service + notification, scheduled sessions, strict-mode toggle.
