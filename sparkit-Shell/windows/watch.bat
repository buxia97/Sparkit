@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

REM ============================================
REM Sparkit - 守护监听 (Windows)
REM 用法: watch.bat [interval] [max_retry]
REM 示例: watch.bat 5 10
REM        watch.bat (默认间隔5秒，最大重试10次)
REM ============================================

set "SCRIPT_DIR=%~dp0"
set "PROJECT_DIR=%SCRIPT_DIR%..\..\sparkit-server"
set "LOG_DIR=%PROJECT_DIR%\logs"
set "PID_FILE=%LOG_DIR%\sparkit.pid"
set "APP_PORT=8083"

REM 轮询间隔（秒），默认 5 秒
set "INTERVAL=5"
if not "%~1"=="" set "INTERVAL=%~1"

REM 最大重试次数，默认 10 次
set "MAX_RETRY=10"
if not "%~2"=="" set "MAX_RETRY=%~2"

echo ============================================
echo  Sparkit 守护监听
echo ============================================
echo  轮询间隔: %INTERVAL% 秒
echo  最大重试: %MAX_RETRY% 次
echo  检测端口: %APP_PORT%
echo ============================================
echo [INFO] 守护进程已启动，按 Ctrl+C 退出
echo.

set "RETRY_COUNT=0"

:watch_loop
REM 检查服务是否存活
set "ALIVE=0"

REM 方式1: 检查 PID 文件
if exist "%PID_FILE%" (
    set /p PID=<"%PID_FILE%"
    tasklist /FI "PID eq !PID!" 2>nul | find /I "!PID!" >nul
    if !errorlevel!==0 set "ALIVE=1"
)

REM 方式2: 检查端口
if !ALIVE!==0 (
    netstat -ano | findstr ":%APP_PORT% " | findstr "LISTENING" >nul
    if !errorlevel!==0 set "ALIVE=1"
)

if !ALIVE!==1 (
    echo [%date% %time%] 服务运行正常
    set "RETRY_COUNT=0"
    goto :sleep
)

REM 服务未存活，尝试拉起
set /a RETRY_COUNT+=1
echo [%date% %time%] [WARN] 服务未运行！正在尝试拉起 (第 !RETRY_COUNT! 次)

if !RETRY_COUNT! GTR !MAX_RETRY! (
    echo [%date% %time%] [ERROR] 已达到最大重试次数 !MAX_RETRY!，退出守护
    echo [%date% %time%] [ALERT] 请检查服务状态并手动排查
    exit /b 1
)

REM 执行启动
call "%SCRIPT_DIR%start.bat"
if !errorlevel!==0 (
    echo [%date% %time%] [SUCCESS] 服务已自动拉起
    set "RETRY_COUNT=0"
) else (
    echo [%date% %time%] [ERROR] 服务拉起失败，将在 %INTERVAL% 秒后重试
)

:sleep
REM 等待轮询间隔
timeout /t %INTERVAL% /nobreak >nul
goto :watch_loop

endlocal