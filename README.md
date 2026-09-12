# Forge

Premium Android media player (Kotlin + Jetpack Compose + Media3).

## Features

- Local video & audio library via MediaStore
- Search and filter (All / Videos / Audio)
- Open network streams (`http`, `https`, `rtsp`)
- Media3 player: play/pause, seek, next/prev, 0.5×–2× speed
- Player gestures: brightness (left), volume (right), horizontal seek
- Resume playback per media URI
- Picture-in-Picture for video
- MediaSession notification / lock-screen controls (background audio)
- Permission flow with skip-to-stream
- `VIEW` intent filters for local and network media
- Premium dark UI (black / graphite + accent)

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
- **version:** 1.1.0 (2)
