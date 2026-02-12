#!/usr/bin/env bash
set -euo pipefail
PKG="${1:-com.hv.cabinet}"
ROUNDS="${2:-30}"

adb shell dumpsys gfxinfo "$PKG" reset
adb shell monkey -p "$PKG" "$ROUNDS" >/tmp/"${PKG//./_}"_monkey.log 2>&1 || true
adb shell dumpsys gfxinfo "$PKG" framestats
