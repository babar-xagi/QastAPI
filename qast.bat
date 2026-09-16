@echo off
setlocal
set ROOT_DIR=%~dp0
set ROOT_DIR=%ROOT_DIR:~0,-1%
set QAST_CWD=%CD%
"%ROOT_DIR%\gradlew.bat" -p "%ROOT_DIR%" -q :qast-cli:run --args="%*"
