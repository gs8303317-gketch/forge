# Forge

Premium Android media player (Kotlin + Jetpack Compose + Media3).

## Features

- Local video & audio library via MediaStore (thumbnail grid + list)
- Reliable MediaStore/SAF thumbnails (`loadThumbnail` + Coil fetcher; album art for audio)
- Immersive player (hides status + navigation bars while playing; restores on leave)
- Continue watching row (unfinished videos with progress on thumb)
- Home-screen playback widget (title + play/pause; Glance)
- Cast entry (MediaRouteButton + Cast framework options; requires Play Services — picker ships; full CastPlayer handoff is best-effort)
- Crash-safe Cast (disabled when Play Services absent; AppCompat-themed MediaRouteButton never kills playback)
- Swipe-to-remove on Continue watching / Recently played
- Playback history screen (full list + clear)
- Default playback speed setting
- Remember last library tab (Video / Audio / Playlists / Browse)
- Back stack: system back matches toolbar (folder → parent, settings/player → previous); exit confirm at root
- Scroll position restored for folders and media lists after back / player
- Playback harden 1.15.0: auto-retry Unexpected runtime errors, isolate optional FX, lyrics MMR only on demand, Coil thumb cache
- Player chrome 1.16.0: longer controls auto-hide (5.5s) paused while menus/dialogs open; optional disable in Settings; lock moved to ⋮ overflow + corner unlock; aspect Fit/Fill/Zoom side rail
- Player chrome 1.17.0: aspect Fit/Fill/Zoom moved to bottom control row (long-press toast); lock unlock auto-hides with chrome (no permanent floating lock); Settings chrome hide delay 3s/5.5s/8s/Never; leaner top bar (speed in ⋮)
- Player power 1.18.0: VLC-like stats overlay; mirror/rotate transform; aspect Fit/Fill/Zoom/16:9/4:3/Original; fine speed 0.25–3× (0.05 steps); quick ±100/±500 sub & audio delay; chapter prev/next; queue bottom sheet; buffering HUD + scrubber buffer; ⋮ shortcuts for EQ/sleep/A-B/snapshot/bookmarks; progress state isolated from full-tree ticks
- Player error UI (message + back — never kill process on media failure)
- Folder browser (MediaStore buckets) with play-folder queue
- Playlists: create / rename / delete, add / remove items, play (DataStore JSON)
- Favorites with home section
- Saved network streams (name + quick open; under Browse)
- Bookmarks per media (named or timestamp; jump from list)
- Media info panel (resolution, duration, size, mime/container, tracks)
- Subtitle delay (± ms) with cue overlay
- Audio delay (Media3 video PTS adjustment for real A/V sync)
- Decoder preference (Auto / Hardware / Software)
- Configurable network load-control buffers + precise seek + skip silence
- Stream quality / video track picker (HLS/DASH variants)
- M3U playlist import / export (local files)
- Volume boost above 100% (LoudnessEnhancer, capped)
- Control lock in ⋮ menu (gestures/controls off; tap to briefly show unlock, same hide timer)
- Frame snapshot to Pictures/Forge
- VLC-style home: Video | Audio | Playlists | Browse bottom navigation
- Sort in a toolbar menu (name / date / size / duration); grid/list toggle
- Browse: folder list, SAF folders, and saved streams
- Recently played home section with clear history (+ optional resume wipe)
- Resume dialog (Continue vs Start over) when position > a few seconds
- Hold player surface for temporary 2× speed
- Subtitle style: size, color, background, vertical position
- Queue reorder (move up/down) in player + playlists
- Share current media via Android share sheet
- Configurable double-tap seek (±5/10/15/30s)
- Per-video brightness memory
- Autoplay next toggle
- Open network streams (`http`, `https`, `rtsp`)
- Media3 player: play/pause, seek, next/prev, 0.25×–3× fine speed
- Multi-band equalizer with presets (system Equalizer API)
- A-B loop (set A / set B / clear)
- Orientation lock while playing (auto follows sensor — portrait clips stay portrait; portrait / landscape locks)
- Subtitles: embedded tracks + external `.srt`/`.vtt`, toggle, size/color/background/position
- Audio track selection
- Repeat (off / one / all) and shuffle
- Double-tap seek (configurable ±5/10/15/30s)
- Aspect ratio: Fit / Fill / Zoom / 16:9 / 4:3 / Original via bottom row (tap cycle; long-press menu)
- Sleep timer (15/30/45/60 min)
- Player gestures: brightness (left 20%), volume (right 20%), horizontal seek (middle); controls stay tappable
- Resume playback per media URI
- Picture-in-Picture for video
- MediaSession notification / lock-screen controls (background audio)
- Permission flow with skip-to-stream
- `VIEW` intent filters for local and network media
- Premium dark UI (black / graphite + accent presets, Material You on 12+)
- Multi-select library items (playlist / favorites / clear)
- Hide folders from the library (manage in Settings)
- Add folders via Storage Access Framework and play those files
- Backup / restore JSON (settings, playlists, favorites, streams, bookmarks)
- Sleep timer fade-out and pause vs stop
- Mini player with Stop/X (clears playback, notification, and service)
- Notification Stop action
- Chapters list + jump when Media3 exposes timed metadata
- Bass boost and Virtualizer toggles (audio session effects)
- Jump to time (hh:mm:ss / mm:ss seek dialog)
- Frame step when paused (approx. 1/fps; hidden while playing)
- Play as audio (disable video track; screen-off friendly)
- Delete media from device (MediaStore / SAF confirm) and library lists
- Folder search (filter items inside the current folder view)
- Quick Settings play/pause tile for the current session
- Gesture sensitivity setting (low / normal / high)

- Video color adjust (brightness / contrast / saturation via TextureView ColorMatrix)
- Audio L/R balance control
- Watched / unwatched mark + filter; auto-mark near end of playback
- Exclude short clips setting (off / 15 / 30 / 60 seconds)
- Stream options: custom User-Agent + network timeout
- Open shared / VIEW text URLs; clipboard paste helper on stream dialog
- Random (shuffle) play all items in current folder
- Hindi UI strings (`values-hi`) + in-app language toggle (System / English / Hindi)

- Audio browsers under Audio tab: Songs / Albums / Artists / Genres (MediaStore); drill-in track lists
- Gapless playback for consecutive audio (Media3 encoder delay/padding)
- Optional audio crossfade (0 / 1s / 2s / 3s soft volume ramp)
- Loudness normalize toggle (LoudnessEnhancer, safe cap with volume boost)
- Optional PIN lock on open (+ biometric when available); gates Settings & Playlists
- Settings: clear image/thumb cache; rough library counts / storage hint
- Lyrics panel: embedded / MediaMetadata description / polished empty state (.lrc best-effort)

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
- **version:** 1.18.0 (20)
