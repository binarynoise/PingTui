#!/usr/bin/sh

cd "$(dirname "$(realpath "$0")")" || exit 1

if [ ! -f ./build/libs/PingTui-shadow-minified.jar ]; then
  ./gradlew shadowJarMinified || exit 1
fi

java -jar ./build/libs/PingTui-shadow-minified.jar
