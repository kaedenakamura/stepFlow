@echo off
REM ============================================================
REM start-demo-user-batch.bat
REM Role: create demo-batch.ON = allow auto user insert (Java reads this file)
REM Need: Spring Boot already running (run-app.bat)
REM ============================================================

cd /d "%~dp0"
REM cd /d "%~dp0" = move to folder where this bat lives (scripts)

if not exist "..\mvnw.cmd" (
    REM check parent folder is stepflow project
    echo ERROR: stepflow project not found. mvnw.cmd missing.
    pause
    exit /b 1
    REM exit /b 1 = quit with error code 1
)

echo ON>demo-batch.ON
REM create flag file; Java DemoBatchFlagService checks Files.exists()

echo.
echo [demo-batch] ENABLED
echo Flag: %cd%\demo-batch.ON
echo.
echo Next steps:
echo   1) App must be running (scripts\run-app.bat)
echo   2) Wait ~10 sec for first user, then every 5 min
echo   3) Stop: scripts\stop-demo-user-batch.bat
echo   4) Check: login as admin, open /users
echo   User name: batch_xxxx  password: BatchPass1
echo.
pause
