# Chloe Learns Math — Legacy Android

A native Android (Kotlin/XML) implementation of Chloe Learns Math, targeting **Android 4.4+ (API 19)**. Bilingual (English/Chinese) with all features matching the main Expo project.

## Why this exists

The main project uses Expo/React Native, which requires Android 7.0+ (API 24). This legacy build supports older Android devices (specifically a KitKat-era tablet).

Both projects are kept in feature parity. Changes are always made in the [main Expo project](../chloe-learns-math) first, then ported here. See `FEATURES.md` for the current feature checklist.

## Key details

- **Chloe's Chinese name is 紫怡**
- **Font:** Bubblegum Sans (loaded from `assets/fonts/`)
- **Language preference:** SharedPreferences key `"app_language"` ("en" or "zh"), stored in `"chloe_prefs"`
- **Audio:** 54 English clips (en/) + 54 Chinese clips (zh/) + laser.mp3
- **Data storage:** All via SharedPreferences with pipe-delimited strings (not JSON)

## Building

```bash
./gradlew assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

**Note:** The `gradlew` script's first two lines fail silently — only the final `java -jar gradle-wrapper.jar` invocation works. If `./gradlew` appears to do nothing, run directly:

```bash
JAVA_HOME="/opt/homebrew/opt/openjdk@17" /opt/homebrew/opt/openjdk@17/bin/java \
  -classpath gradle/wrapper/gradle-wrapper.jar \
  org.gradle.wrapper.GradleWrapperMain assembleDebug
```

## Project structure

```
app/src/main/
├── kotlin/com/example/chloelearns/
│   ├── MainActivity.kt       # Home screen (bouncing chars, flag selector, today counter)
│   ├── MathGameActivity.kt   # Game screen (timer, keypad, scoring, bilingual audio/text)
│   ├── HistoryActivity.kt    # Game history table (bilingual)
│   └── StatsActivity.kt      # Per-question stats with expandable mistake details (bilingual)
├── res/
│   ├── layout/
│   │   ├── activity_main.xml       # Home screen layout
│   │   └── activity_math_game.xml  # Game screen layout
│   └── values/
│       └── styles.xml              # NoActionBar theme
└── assets/
    ├── audio/
    │   ├── en/        # 54 English clips (Ana voice)
    │   ├── zh/        # 54 Chinese clips (Xiaoxiao voice, uses 紫怡)
    │   └── laser.mp3
    ├── images/
    │   ├── characters/   # 17 bouncing character PNGs
    │   ├── celebration/  # 16 celebration PNGs
    │   ├── menu/         # Button blobs, flags, heart, blackboard, unicorn
    │   └── explosion.png
    └── fonts/
        └── BubblegumSans-Regular.ttf
```

## Data formats (SharedPreferences)

All stored in `"chloe_prefs"`:

| Key | Format |
|---|---|
| `math_score` | int |
| `app_language` | "en" or "zh" |
| `game_history` | Newline-separated: `game\|mode\|correct\|total\|secs\|date` |
| `question_stats` | Newline-separated: `key\|score\|attempts\|game\|num1\|num2` |
| `mistake_log` | Newline-separated: `key\|givenAnswer\|correctAnswer\|game\|num1\|num2\|date` |
