@echo off
REM ============================================================
REM 01_practice_echo.bat … バッチの練習用（いちばん簡単）
REM 使い方: エクスプローラーでダブルクリック、または
REM         PowerShell で  scripts\01_practice_echo.bat
REM ============================================================

REM 画面の文字化けを減らす（UTF-8）。日本語Windowsでも大抵動く
chcp 65001 > nul

echo.
echo === StepFlow バッチ練習 ===
echo これはバッチの練習用です。
echo 今の日時: %date% %time%
echo このファイルの場所: %~dp0
echo.

REM 変数： set 名前＝値(=の前後にスペースを入れない)
set PROJECT_NAME=stepflow
echo プロジェクト（変数）: %PROJECT_NAME%

REM　引数： 実行時に 01_practice_echo.bat 太郎 と付けると %1 が 太郎
if "%~1"==""(
    echo ヒント：引数付きで試す→ 01_practice_echo.bat 太郎
) else (
    echo 渡された引数 %%1 = %~1
)
echo.
echo 親フォルダ（プロジェクト直下）へ移動する例:
cd /d "%~dp0\.."
echo 現在のフォルダ: %cd%

echo.
pause
