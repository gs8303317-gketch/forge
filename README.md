# Forge

Premium Android media player (Kotlin + Jetpack Compose + Media3).

## Features

- Local video & audio library via MediaStore (thumbnail grid + list)
- Folder browser (MediaStore buckets) with play-folder queue
- Playlists: create / rename / delete, add / remove items, play (DataStore JSON)
- Favorites with home section
- Search and filter (All / Videos / Audio)
- Recently played home section
- Open network streams (`http`, `https`, `rtsp`)
- Media3 player: play/pause, seek, next/prev, 0.5×–2× speed
- Multi-band equalizer with presets (system Equalizer API)
- A-B loop (set A / set B / clear)
- Orientation lock while playing (auto / portrait / landscape)
- Subtitles: embedded tracks + external `.srt`/`.vtt`, toggle, size
- Audio track selection
- Repeat (off / one / all) and shuffle
- Double-tap seek (−10s / +10s)
- Aspect ratio: Fit / Fill / Zoom
- Sleep timer (15/30/45/60 min)
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
- **version:** 1.3.0 (4)
