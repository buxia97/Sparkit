@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

REM ============================================
REM Sparkit - 日志实时监控 (Windows)
REM 用法: log-monitor.bat [keyword]
REM 示例: log-monitor.bat
REM       log-monitor.bat ERROR
REM       log-monitor.bat "AccessToken"
REM ============================================

set "SCRIPT_DIR=%~dp0"
set "PROJECT_DIR=%SCRIPT_DIR%..\..\sparkit-server"
set "LOG_DIR=%PROJECT_DIR%\logs"
set "LOG_FILE=%LOG_DIR%\sparkit.log"

if not exist "%LOG_FILE%" (
    echo [ERROR] 日志文件不存在: %LOG_FILE%
    echo [INFO] 请先启动服务: start.bat
    exit /b 1
)

set "KEYWORD=%~1"

echo ============================================
echo  Sparkit 日志实时监控
echo ============================================
echo  日志文件: %LOG_FILE%
if not "%KEYWORD%"=="" (
    echo  过滤关键字: %KEYWORD%
    echo ============================================
    echo [INFO] 按 Ctrl+C 退出监控
    echo.

    REM 使用 PowerShell 实现类似 tail -f 的效果
    powershell -Command "$logFile='%LOG_FILE%'; $keyword='%KEYWORD%'; $lastSize=(Get-Item $logFile).Length; while($true){Clear-Host; Write-Host '=== Sparkit 日志监控 (关键字: '$keyword') ===' -ForegroundColor Cyan; $lines=Get-Content $logFile -Tail 50; foreach($line in $lines){if($line -match $keyword){Write-Host $line -ForegroundColor Red}else{Write-Host $line}}; Start-Sleep -Seconds 2}"
) else (
    echo ============================================
    echo [INFO] 按 Ctrl+C 退出监控
    echo.

    powershell -Command "$logFile='%LOG_FILE%'; while($true){Clear-Host; Write-Host '=== Sparkit 日志监控 ===' -ForegroundColor Cyan; Get-Content $logFile -Tail 50; Start-Sleep -Seconds 2}"
)

endlocal