@echo off
REM Start Spring Boot. Stop with Ctrl+C in this window.

cd /d "%~dp0\.."

echo [run-app] folder: %cd%
echo [run-app] starting... open http://localhost:8080
echo [run-app] press Ctrl+C to stop
echo.

call mvnw.cmd spring-boot:run

if errorlevel 1 (
    echo.
    echo [run-app] FAILED to start
    pause
    exit /b 1
)

pause
