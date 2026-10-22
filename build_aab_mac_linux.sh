#!/usr/bin/env bash
set -e
cd "$(dirname "$0")"
echo "================================================================="
echo "  Time Table Manager v2.1 - 1-Click Mobile .APK & Play Store .AAB"
echo "================================================================="
if [ -f "./gradlew" ]; then
    chmod +x ./gradlew
    ./gradlew assembleDebug assembleRelease bundleRelease
else
    gradle assembleDebug assembleRelease bundleRelease
fi
echo "================================================================="
echo "1. Mobile Test APK: app/build/outputs/apk/debug/app-debug.apk"
echo "2. Play Store AAB:  app/build/outputs/bundle/release/app-release.aab"
echo "================================================================="
