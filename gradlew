#!/usr/bin/env sh
set -eu
GRADLE_VERSION="8.2"
CACHE="${HOME}/.gradle/winols-gradle-${GRADLE_VERSION}"
if [ ! -x "$CACHE/bin/gradle" ]; then
  mkdir -p "$CACHE"
  TMP="${TMPDIR:-/tmp}/winols-gradle.zip"
  URL="https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip"
  echo "Downloading Gradle ${GRADLE_VERSION}..."
  curl -fsSL "$URL" -o "$TMP"
  rm -rf "$CACHE/unpacked"
  mkdir -p "$CACHE/unpacked"
  unzip -q "$TMP" -d "$CACHE/unpacked"
  mv "$CACHE/unpacked/gradle-${GRADLE_VERSION}"/* "$CACHE/"
  rm -rf "$CACHE/unpacked" "$TMP"
fi
exec "$CACHE/bin/gradle" "$@"