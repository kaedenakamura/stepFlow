@echo off
REM Run: mvnw.cmd clean compile (from project root)

cd /d "%~dp0\.."

echo [compile] folder: %cd%
echo [compile] running mvnw.cmd clean compile ...
echo.

call mvnw.cmd clean compile
if errorlevel 1 (
    echo.
    echo [compile] FAILED - read ERROR lines above
    pause
    exit /b 1
)

echo.
echo [compile] SUCCESS
pause
exit /b 0
