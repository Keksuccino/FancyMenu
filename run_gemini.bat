@echo off
setlocal
:: Resolve the project directory based on this script's location
set "PROJECT_DIR=%~dp0"
if "%PROJECT_DIR:~-1%"=="\" set "PROJECT_DIR=%PROJECT_DIR:~0,-1%"

for /f "usebackq tokens=* delims=" %%I in (`wsl.exe wslpath "%PROJECT_DIR%"`) do set "WSL_PROJECT_DIR=%%I"

if not defined WSL_PROJECT_DIR (
    echo Failed to resolve WSL path for "%PROJECT_DIR%".
    pause
    exit /b 1
)

wsl.exe -e bash -lic "cd '%WSL_PROJECT_DIR%' && gemini"
pause
