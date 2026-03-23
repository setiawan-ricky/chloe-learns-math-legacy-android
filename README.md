# Chloe Learns Math — Legacy Android

A native Android (Kotlin) implementation of [Chloe Learns Math](https://github.com/setiawan-ricky/chloe-learns-math), targeting **Android 4.4+ (API 19)**.

## Why this exists

The main project uses Expo/React Native, which requires Android 7.0+ (API 24) at minimum. This legacy build exists to support older Android devices (specifically a KitKat-era tablet) that can't run React Native.

Both projects are kept in feature parity. Changes are always made in the [main project](https://github.com/setiawan-ricky/chloe-learns-math) first, then ported here. See `FEATURES.md` for the current feature checklist.

## Building

Open the project in Android Studio or build from the command line:

```
./gradlew assembleDebug
```

The APK will be at `app/build/outputs/apk/debug/app-debug.apk`.
