@echo off
REM 本番用 Docker Compose 起動（ビルド込み）

cd /d "%~dp0\.."

if not exist ".env.prod" (
    echo [run-prod-docker] .env.prod がありません。
    echo   copy .env.prod.example .env.prod
    echo   を実行し、パスワードを変更してください。
    pause
    exit /b 1
)

echo [run-prod-docker] building and starting...
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build

if errorlevel 1 (
    echo [run-prod-docker] FAILED
    pause
    exit /b 1
)

echo.
echo [run-prod-docker] started. open http://localhost:9138/login
echo   admin / password
echo   店長 / password
echo   倉庫長 / password
pause
