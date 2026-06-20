@echo off
REM 本番用 Docker Compose 停止

cd /d "%~dp0\.."

if not exist ".env.prod" (
    echo [stop-prod-docker] .env.prod がありません。copy .env.prod.example .env.prod を先に実行してください。
    pause
    exit /b 1
)

docker compose -f docker-compose.prod.yml --env-file .env.prod down

echo [stop-prod-docker] stopped.
pause
