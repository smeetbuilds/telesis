#!/bin/sh

#
# Telesis Gradle wrapper launcher.
# Runs the Gradle wrapper JAR from gradle/wrapper/ and forwards all arguments.
#

APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)

if [ -n "$JAVA_HOME" ]; then
  JAVACMD="$JAVA_HOME/bin/java"
else
  JAVACMD="java"
fi

if ! command -v "$JAVACMD" >/dev/null 2>&1; then
  echo "ERROR: Java is required to run Gradle. Install JDK 17+ or set JAVA_HOME." >&2
  exit 1
fi

exec "$JAVACMD" -classpath "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"
