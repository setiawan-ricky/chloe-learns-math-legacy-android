#!/bin/bash
set -e

cd "$(dirname "$0")"

JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@17}"
export JAVA_HOME

echo "Building debug APK..."
"$JAVA_HOME/bin/java" -classpath gradle/wrapper/gradle-wrapper.jar \
  org.gradle.wrapper.GradleWrapperMain assembleDebug --console=plain

echo "Installing on device..."
adb install -r app/build/outputs/apk/debug/app-debug.apk

echo "Launching app..."
adb shell am start -n com.example.chloelearns/.MainActivity

echo "Done."
