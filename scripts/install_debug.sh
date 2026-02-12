#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am force-stop com.hv.cabinet || true
adb shell monkey -p com.hv.cabinet -c android.intent.category.LAUNCHER 1
