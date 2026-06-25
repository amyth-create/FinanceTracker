#!/bin/sh
# Gradle wrapper script
exec "$(dirname "$0")/gradle/wrapper/gradlew" "$@" 2>/dev/null || {
  # Fallback: try system gradle or prompt
  echo "Run: gradle wrapper --gradle-version 8.4 to generate wrapper, or open in Android Studio."
}
