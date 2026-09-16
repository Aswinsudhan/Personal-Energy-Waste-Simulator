@echo off
setlocal
cd /d "%~dp0"

where node >nul 2>&1
if errorlevel 1 (
    echo Node.js is not installed on this computer.
    echo Please install the LTS version from https://nodejs.org/
    start "" "https://nodejs.org/en/download"
    pause
    exit /b 1
)

start "Personal Energy Simulator Server" /min cmd /c "node server.js"
timeout /t 2 /nobreak >nul
start "" "http://localhost:3000"

echo Personal Energy Waste Simulator is open in your browser.
echo You can close this window when you are finished.
pause >nul
