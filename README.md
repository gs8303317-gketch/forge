# Forge

Premium Android media player (Kotlin + Jetpack Compose + Media3).

## Features (Phase 1)

- Local video & audio library via MediaStore
- Search and filter (All / Videos / Audio)
- Media3 (ExoPlayer) player with play/pause, seek, next/prev
- Permission flow for media access
- `VIEW` intent filters for `video/*` and `audio/*`
- Premium dark UI (black / graphite + accent)
- Settings / About with version info

## Build

```bash
./gradlew :app:assembleDebug
```

Release signing (CI) uses env vars:

- `KEYSTORE_FILE`
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

## App

- **applicationId:** `com.gketch.forge`
- **minSdk:** 26 · **targetSdk / compileSdk:** 35
- **version:** 1.0.0 (1)
