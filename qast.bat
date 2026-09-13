@echo off
setlocal
set SCRIPT_DIR=%~dp0
set QAST_CWD=%CD%
"%SCRIPT_DIR%gradlew.bat" -q :qast-cli:run --args="%*"
