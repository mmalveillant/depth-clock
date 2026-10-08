<h1 align="center">Depth Clock</h1>

<p align="center">
  Put the lock-screen clock <b>behind</b> the person in your photo — the iOS “depth effect” — on Samsung Galaxy and Android 14+.
</p>

## What it does

Pick a photo from your gallery. The app separates the person from the background on-device and draws the lock-screen clock between the background and the subject, so the time looks like it sits behind the person’s head — just like iOS 16 and later.

Samsung does not offer this for gallery photos: One UI’s *Adaptive Clock* bends the digits **around** the subject, never behind it.

## Features

- **On-device subject segmentation** with ML Kit Subject Segmentation — your photo never leaves the phone.
- **24 clock presets**: iPhone, tall & thin, stacked, two-tone, outline, digital, pixel, chalk, and more.
- **27 fonts**: the phone’s variable system fonts (Roboto Flex, One UI Sans, Samsung numerals) plus 17 open-source fonts.
- **Full control**: size, height, width, weight, digit spacing, line gap, opacity.
- **Colours from your photo**, curated swatches, a custom hue/saturation picker, and dimmed or outlined minutes.
- **Drag the clock anywhere** with centre snapping, pinch to resize, and frame the photo by dragging and pinching.
- **WYSIWYG preview**: the in-app preview and the real lock screen are drawn by the same renderer.
- A warning when the subject covers most of the clock.
- Pick straight from Samsung Gallery, or share an image to the app from any gallery.

## How it works

The app installs a **live wallpaper** (`WallpaperService`) on the **lock screen only**, which draws in this order:

```
photo  →  clock  →  subject cutout  →  date
```

It redraws once a minute (`ACTION_TIME_TICK`), so the battery impact is negligible.

## Install & setup

1. Download the APK from [Releases](../../releases) and install it (Android 14 or newer).
2. Open the app, pick a photo, style the clock, tap **Apply to lock screen**, and choose **Lock screen**.
3. **Hide Samsung’s own clock** (one time): long-press the lock screen → tap the clock → *Font and colour*:
   - pick a plain font (not one of the *animated clocks*),
   - turn **Blur** off (the drop icon at the start of the colours),
   - open the colour picker and set **Opacity to 0%**,
   - shrink the clock frame and drag it into the top corner (One UI keeps a faint fixed shadow under its clock).
4. If notifications cover the clock: *Settings → Lock screen → Lock screen notifications* → **Dot** or **Icons**.

> ⚠️ Force-stopping the app or clearing its data makes Android remove the wallpaper from the lock screen — this is standard Android behaviour for every live wallpaper.

## Build

```bash
./gradlew assembleRelease
```

The APK is written to `app/build/outputs/apk/release/`. Requires JDK 17+ and Android SDK 36.

## Tech stack

Kotlin · Jetpack Compose · Material 3 · WallpaperService · ML Kit Subject Segmentation · variable fonts (`wght` / `wdth`)

| Package | Purpose |
|---|---|
| `render/` | `LockRenderer` and `ClockRenderer` — one renderer for the preview and the lock screen |
| `wallpaper/` | The lock-screen live wallpaper |
| `data/` | Design model (`ClockSpec`, `DepthDesign`) and atomic storage |
| `image/` | Photo decoding and subject segmentation |
| `ui/` | The Compose editor |

## Fonts

Fonts in `app/src/main/assets/fonts` are licensed under the [SIL Open Font License 1.1](https://openfontlicense.org) — see `OFL-NOTICE.txt`.
Roboto Flex, One UI Sans and the Samsung numeral fonts are read from the device (`/system/fonts`) and are not bundled.
