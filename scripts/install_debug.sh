#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

TARGET="${1:-}"
PACKAGE="com.hv.cabinet"
APK_PATH="app/build/outputs/apk/debug/app-debug.apk"

ADB_ARGS=()
if [[ -n "$TARGET" ]]; then
  if [[ "$TARGET" != *:* ]]; then
    TARGET="${TARGET}:5555"
  fi
  adb connect "$TARGET" >/dev/null || true
  ADB_ARGS=(-s "$TARGET")
fi

./gradlew assembleDebug
adb "${ADB_ARGS[@]}" install -r "$APK_PATH"
# Debug 包首次安装后容易出现未优化执行，先做 speed 编译以缩短冷启动时间。
if ! adb "${ADB_ARGS[@]}" shell cmd package compile -f -m speed "$PACKAGE"; then
  echo "[warn] package compile 失败，继续执行安装后的启动验证。"
fi
adb "${ADB_ARGS[@]}" shell am force-stop "$PACKAGE" || true
adb "${ADB_ARGS[@]}" shell monkey -p "$PACKAGE" -c android.intent.category.LAUNCHER 1
