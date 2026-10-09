@echo off
cd /d "%~dp0"
call gradlew.bat runLocalServer
pause
