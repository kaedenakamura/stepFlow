@echo off
REM ============================================================
REM stop-demo-user-batch.bat
REM Role: delete demo-batch.ON = no more auto user insert
REM Note: does NOT stop Spring Boot (use Ctrl+C in run-app window)
REM ============================================================

cd /d "%~dp0"

if exist demo-batch.ON (
    del demo-batch.ON
    REM del = delete file
    echo [demo-batch] STOPPED (flag removed)
) else (
    echo [demo-batch] Already OFF
)

echo.
echo Note: Spring Boot may still run.
echo To stop the app: Ctrl+C in run-app.bat window
echo.
pause
