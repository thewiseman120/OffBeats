# OffBeats 🎵

OffBeats is a minimal offline music player for Android, designed around a soft, professional palette:

- `#EDAFB8`
- `#F7E1D7`
- `#DEDBD2`
- `#B0C4B1`
- `#4A5759`

## Features

- Plays offline songs from device storage
- Modern library + now-playing layout
- Play / pause / next / previous controls
- Seek bar with live progress updates
- Smooth, bouncy micro-animations on controls and active tracks
- Built with Kotlin + Jetpack Compose + Media3 ExoPlayer

## Build APK on GitHub (no local Android setup needed)

1. Push this repo to GitHub.
2. Open **Actions** tab.
3. Run **Build Android APK** workflow.
4. Download `offbeats-debug-apk` artifact from the workflow run.

## Local project

- App module: `app/`
- Main UI: `app/src/main/java/com/offbeats/player/MainActivity.kt`
- Playback state: `app/src/main/java/com/offbeats/player/MusicPlayerViewModel.kt`
- CI workflow: `.github/workflows/android-apk.yml`
