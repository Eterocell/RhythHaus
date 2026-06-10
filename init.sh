#!/bin/bash
set -euo pipefail

echo "=== RhythHaus harness verification ==="
echo "Project: $(basename "$(pwd)")"
echo ""

echo "=== Shared JVM tests + desktop compile + Android debug build ==="
./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache

echo ""
echo "=== Xcode toolchain ==="
/usr/bin/xcrun xcodebuild -version

echo ""
echo "=== iOS simulator shared tests ==="
./gradlew :shared:iosSimulatorArm64Test --configuration-cache

echo ""
echo "=== Harness verification complete ==="
