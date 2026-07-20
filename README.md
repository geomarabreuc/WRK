# WRK

Minimal Android deep-work timer. Start a session and the app pins itself fullscreen (Android screen pinning), making the phone unusable until the countdown ends.

Black-and-white, no accounts, no network, no ads. Built for personal use.

## Features

- Hours + minutes timer — steppers or tap a numeral to type
- Screen pinning during a session; escape shows Resume / End controls
- Named task templates (tap to apply, hold to delete)
- Looping alarm + vibration on completion
- Work-day calendar with daily minute goal and streak
- Session survives screen-off, rotation, and process death (wall-clock based)

## Build

Kotlin + Jetpack Compose, single module. Requires JDK 17 and the Android SDK.

```bash
./gradlew installDebug   # build + install on a USB-connected phone
```

On the phone, enable **App pinning** (Settings → Security) for the lock to work.

## Screenshots

Black screen, big numbers. You get the idea.
