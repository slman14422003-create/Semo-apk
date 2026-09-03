#!/bin/sh
# Standard Gradle wrapper launcher.
# NOTE: gradle/wrapper/gradle-wrapper.jar is not bundled in this export
# (no network access when this project was generated). Before using this
# script, run `gradle wrapper` once (with any local Gradle install) or
# simply open the project in Android Studio, which will fetch/regenerate
# the wrapper jar automatically on first sync.

DIR="$(cd "$(dirname "$0")" && pwd)"
exec gradle "$@" -p "$DIR"
