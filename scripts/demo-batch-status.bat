@echo off
REM ============================================================
REM demo-batch-status.bat
REM Role: show ON/OFF by checking if demo-batch.ON exists
REM ============================================================

cd /d "%~dp0"

echo === Demo batch status ===
if exist demo-batch.ON (
    echo State: ON  (users added every 5 min while app runs)
    echo Flag file exists: %cd%\demo-batch.ON
) else (
    echo State: OFF
    echo To enable: scripts\start-demo-user-batch.bat
)
echo.
pause
