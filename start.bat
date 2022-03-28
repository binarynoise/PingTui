if NOT EXIST %~dp0\build\libs\PingTui-standalone.jar %~dp0\gradlew shadowJar || ( pause && exit )

java -jar %~dp0\build\libs\PingTui-standalone.jar || pause
