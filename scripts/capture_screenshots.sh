#!/usr/bin/env bash
# Capture app feature screenshots from a running emulator.
set -euo pipefail

ADB="${ANDROID_HOME:-$HOME/Library/Android/sdk}/platform-tools/adb"
DEVICE="${1:-emulator-5554}"
OUT_DIR="$(cd "$(dirname "$0")/.." && pwd)/docs/screenshots"

mkdir -p "$OUT_DIR"

tap() {
  "$ADB" -s "$DEVICE" shell input tap "$1" "$2"
}

screenshot() {
  local name="$1"
  sleep 1.5
  "$ADB" -s "$DEVICE" exec-out screencap -p > "$OUT_DIR/$name"
  echo "Saved $OUT_DIR/$name"
}

back() {
  "$ADB" -s "$DEVICE" shell input keyevent KEYCODE_BACK
  sleep 0.8
}

launch() {
  "$ADB" -s "$DEVICE" shell am start -n com.tradingapp/.MainActivity
  sleep 3
}

launch

# 1. Market watchlist (default tab)
screenshot "watchlist.png"

# 2. Favorites tab
tap 540 2256
screenshot "favorites.png"

# 3. Back to Market tab
tap 173 2256
sleep 1

# 4. Search
tap 881 227
screenshot "search.png"
back

# 5. Market detail (tap BTC row)
tap 540 405
screenshot "detail.png"

# 6. Trading (trade icon in top bar)
tap 1000 227
screenshot "trading.png"
back
back

# 7. Portfolio tab
tap 907 2256
screenshot "portfolio.png"

# 8. Settings (from Market tab)
tap 173 2256
sleep 1
tap 1007 227
screenshot "settings.png"

echo "Done. Screenshots in $OUT_DIR"
