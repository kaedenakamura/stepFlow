@echo off
REM ============================================================
REM dev-check.bat … 開発前の簡易チェック（コンパイル → 結果表示）
REM A4の一括発注を入れる前に「壊れてないか」確認する用
REM ============================================================
chcp 65001 >nul
cd /d "%cd%\.."

echo ========================================
echo  StepFlow dev-check
echo  %date% %time%
echo ========================================
echo.

echo [1/1] compile ...
call mvnw.cmd -q clean compile

if errorlevel 1 (
    echo.
    echo 結果： NG (コンパイルエラーあり)
    echo 対処： ターミナルに出た ERROR 行を IDE で該当ファイルを開いて修正
    pause
    exit /b 1
)

echo.
echo 結果: OK  （コンパイル成功）
echo 次: run-app.bat で起動して画面確認
echo.
pause
exit /b 0
