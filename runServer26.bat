@echo off
set "JAVA_HOME=C:\Projects\tiersmp\jdk25_root\jdk-25.0.5+7"
set "PATH=%JAVA_HOME%\bin;%PATH%"
echo Starting Purpur 26.2 Server on Java 25...
call .\gradlew.bat runServer %*
