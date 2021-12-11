#!/usr/bin/sh

cd "$(dirname "$(realpath "$0")")" || exit 1

./gradlew shadowJar && java -jar ./build/libs/PingTui-standalone.jar
