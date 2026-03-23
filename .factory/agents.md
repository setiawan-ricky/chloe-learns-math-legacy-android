# Agent Instructions

This is the legacy native Android (Kotlin) implementation of Chloe Learns Math, targeting Android 4.4+ (API 19).

## Cross-Project Sync

The canonical implementation is the Expo React Native project at `../chloe-learns`. Do not make changes here first. The workflow is:

1. All feature and behavior changes are made in the Expo project first.
2. Changes are then ported to this project.
3. Both `FEATURES.md` files are kept in sync.

When porting a change, adapt it to native Android APIs (MediaPlayer for audio, Android Views for UI, SharedPreferences for storage). If a feature cannot be implemented due to API 19 constraints, mark it with `[ ]` in this project's `FEATURES.md` and add a note explaining why.
