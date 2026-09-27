#!/usr/bin/env bash
#
# Rasterises every launcher icon density and the splash artwork from the vector
# sources committed under app/src/main/assets.
#
# Keeping a single vector as the source of truth (rather than committing a folder
# full of PNGs) means every density is derived during the build and the bitmaps can
# never drift apart. The only tool required is rsvg-convert.

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
RES="$ROOT/app/src/main/res"
ASSETS="$ROOT/app/src/main/assets"
LOGO="$ASSETS/nusantara_logo.svg"
SPLASH="$ASSETS/nusantara_splash.svg"

if [ ! -f "$LOGO" ]; then
  echo "Logo vector not found at $LOGO" >&2
  exit 1
fi

# Legacy launcher icon edge in px per density:
#   mdpi 48, hdpi 72, xhdpi 96, xxhdpi 144, xxxhdpi 192
# The adaptive-icon foreground layer is drawn on the 108dp canvas the platform
# expects, so it scales by the same factor (1x, 1.5x, 2x, 3x, 4x).
declare -A ICON=( [mdpi]=48 [hdpi]=72 [xhdpi]=96 [xxhdpi]=144 [xxxhdpi]=192 )
declare -A FG=( [mdpi]=108 [hdpi]=162 [xhdpi]=216 [xxhdpi]=324 [xxxhdpi]=432 )

for d in "${!ICON[@]}"; do
  dir="$RES/mipmap-$d"
  mkdir -p "$dir"
  icon="${ICON[$d]}"
  fg="${FG[$d]}"

  rsvg-convert -w "$icon" -h "$icon" "$LOGO" -o "$dir/ic_launcher.png"
  rsvg-convert -w "$icon" -h "$icon" "$LOGO" -o "$dir/ic_launcher_round.png"
  rsvg-convert -w "$fg"   -h "$fg"   "$LOGO" -o "$dir/ic_launcher_foreground.png"
  echo "generated mipmap-$d (icon ${icon}px, foreground ${fg}px)"
done

mkdir -p "$RES/drawable-nodpi"

# Splash logo shown in the greeting block.
rsvg-convert -w 512 -h 512 "$LOGO" -o "$RES/drawable-nodpi/logo_nusantara.png"
echo "generated drawable-nodpi/logo_nusantara.png"

# Splash hero banner, rendered straight to a portrait bitmap.
if [ -f "$SPLASH" ]; then
  rsvg-convert -w 1080 -h 1920 "$SPLASH" -o "$RES/drawable-nodpi/splash_hero.png"
  echo "generated drawable-nodpi/splash_hero.png"
fi

echo "Icon generation complete."
