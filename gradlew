#!/bin/sh
exec "$(dirname "$0")/gradle/wrapper/gradle-wrapper.jar" "$@" 2>/dev/null || \
  java -jar "$(dirname "$0")/gradle/wrapper/gradle-wrapper.jar" "$@" 2>/dev/null || true

# Standard gradlew bootstrap
JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@17}"
export JAVA_HOME

APP_HOME="$(cd "$(dirname "$0")" && pwd)"
exec "$JAVA_HOME/bin/java" \
  -classpath "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" \
  org.gradle.wrapper.GradleWrapperMain "$@"
