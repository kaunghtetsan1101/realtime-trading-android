#!/usr/bin/env bash
# Record a feature walkthrough video from a running emulator (1080x2424).
# Usage: ./scripts/record_feature_demo.sh [device_serial]
set -euo pipefail

ADB="${ANDROID_HOME:-$HOME/Library/Android/sdk}/platform-tools/adb"
DEVICE="${1:-emulator-5554}"
ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
OUT_DIR="$ROOT_DIR/docs/videos"
REMOTE_PATH="/sdcard/trading_feature_demo.mp4"
LOCAL_PATH="$OUT_DIR/feature_demo.mp4"
TIME_LIMIT="${TIME_LIMIT:-100}"

mkdir -p "$OUT_DIR"

tap() {
  "$ADB" -s "$DEVICE" shell input tap "$1" "$2"
}

swipe() {
  "$ADB" -s "$DEVICE" shell input swipe "$1" "$2" "$3" "$4" "${5:-400}"
}

back() {
  "$ADB" -s "$DEVICE" shell input keyevent KEYCODE_BACK
  sleep 0.8
}

type_text() {
  "$ADB" -s "$DEVICE" shell input text "$1"
}

launch() {
  "$ADB" -s "$DEVICE" shell am force-stop com.tradingapp
  sleep 0.5
  "$ADB" -s "$DEVICE" shell am start -n com.tradingapp/.MainActivity
  sleep 4
}

wait_s() {
  sleep "$1"
}

if ! "$ADB" -s "$DEVICE" get-state >/dev/null 2>&1; then
  echo "Device $DEVICE is not available. Start an emulator or connect a device."
  exit 1
fi

echo "Recording ${TIME_LIMIT}s demo on $DEVICE → $LOCAL_PATH"

# Start screen capture on the device (blocks until time limit or interrupt).
"$ADB" -s "$DEVICE" shell screenrecord --time-limit "$TIME_LIMIT" "$REMOTE_PATH" &
RECORD_PID=$!

cleanup() {
  if kill -0 "$RECORD_PID" 2>/dev/null; then
    wait "$RECORD_PID" 2>/dev/null || true
  fi
}
trap cleanup EXIT

launch

# --- 1. Market Watch: live prices + scroll ---
wait_s 2
swipe 540 1600 540 700 500
wait_s 1.5
swipe 540 700 540 1600 500
wait_s 1

# Favorite BTC (star on first row, right side)
tap 1000 405
wait_s 1.5

# --- 2. Watchlist (Favorites) tab ---
tap 540 2256
wait_s 3

# --- 3. Market tab ---
tap 173 2256
wait_s 1.5

# --- 4. Search ---
tap 881 227
wait_s 1
tap 540 350
type_text "eth"
wait_s 2.5
tap 540 550
wait_s 3

# --- 5. Market detail (from search result) ---
swipe 540 1400 540 900 400
wait_s 2

# --- 6. Trading flow ---
tap 1000 227
wait_s 2
tap 540 1050
type_text "0.01"
wait_s 1
tap 300 1280
wait_s 1
tap 540 2100
wait_s 2
tap 540 2050
wait_s 2.5
back
wait_s 1
back
wait_s 1

# --- 7. Portfolio ---
tap 907 2256
wait_s 3
swipe 540 1500 540 800 400
wait_s 2

# --- 8. Settings + dark theme ---
tap 173 2256
wait_s 1
tap 1007 227
wait_s 2
tap 700 520
wait_s 2
back
wait_s 1.5

# --- 9. Market watch in dark theme (show realtime ticks) ---
wait_s 4

wait "$RECORD_PID" || true
RECORD_PID=""

echo "Pulling recording..."
"$ADB" -s "$DEVICE" pull "$REMOTE_PATH" "$LOCAL_PATH"
"$ADB" -s "$DEVICE" shell rm -f "$REMOTE_PATH"

BYTES=$(wc -c < "$LOCAL_PATH" | tr -d ' ')
if [[ "$BYTES" -lt 10000 ]]; then
  echo "Recording looks too small (${BYTES} bytes). Check emulator taps or increase TIME_LIMIT."
  exit 1
fi

echo "Done. Video saved to $LOCAL_PATH ($(du -h "$LOCAL_PATH" | cut -f1))"
