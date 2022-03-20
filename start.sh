#!/usr/bin/sh

cd "$(dirname "$(realpath "$0")")" || exit 1

if [ ! -f ./build/libs/PingTui-standalone.jar ]; then
  ./gradlew shadowJar || exit 1
fi

java -jar ./build/libs/PingTui-standalone.jar
