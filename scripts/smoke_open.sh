#!/usr/bin/env bash
set -euo pipefail
adb shell monkey -p com.hv.cabinet -c android.intent.category.LAUNCHER 1
