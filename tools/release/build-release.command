#!/bin/bash
# Dvojklik ve Finderu: spustí testy, sestaví podepsané release APK a zkopíruje ho do releases/.
set -euo pipefail
cd "$(dirname "$0")/../.."

export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home}"
VERSION=$(sed -n 's/.*versionName = "\(.*\)".*/\1/p' app/build.gradle.kts)
APK="releases/app-release-$VERSION.apk"

finish() { echo; read -r -p "Stiskni Enter pro zavření…" _; }
trap finish EXIT

echo "== Heller $VERSION: testy + release build =="
[ -f keystore.properties ] || { echo "CHYBA: chybí keystore.properties (APK by bylo podepsané debug klíčem)."; exit 1; }

./gradlew :app:testDebugUnitTest :app:assembleRelease
cp app/build/outputs/apk/release/app-release.apk "$APK"

echo
echo "HOTOVO: $APK ($(du -h "$APK" | cut -f1))"
