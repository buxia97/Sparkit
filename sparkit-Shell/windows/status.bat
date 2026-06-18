@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

REM ============================================
REM Sparkit - 查看服务状态 (Windows)
REM 用法: status.bat
REM ============================================

set "SCRIPT_DIR=%~dp0"
set "PROJECT_DIR=%SCRIPT_DIR%..\..\sparkit-server"
set "LOG_DIR=%PROJECT_DIR%\logs"
set "PID_FILE=%LOG_DIR%\sparkit.pid"
set "APP_PORT=8083"

echo ============================================
echo  Sparkit 服务状态
echo ============================================

REM 检查 PID 文件
set "PID="
set "ALIVE=0"

if exist "%PID_FILE%" (
    set /p PID=<"%PID_FILE%"
    echo [INFO] PID 文件: %PID_FILE%
    echo [INFO] 记录 PID: !PID!

    tasklist /FI "PID eq !PID!" 2>nul | find /I "!PID!" >nul
    if !errorlevel!==0 set "ALIVE=1"
)

REM 检查端口
echo.
echo [INFO] 端口 %APP_PORT% 监听状态:
netstat -ano | findstr ":%APP_PORT% " | findstr "LISTENING" >nul
if !errorlevel!==0 (
    echo   状态: LISTENING
    set "ALIVE=1"
) else (
    echo   状态: 未监听
)

REM 综合判断
echo.
echo ============================================
if !ALIVE!==1 (
    echo  服务状态: 运行中

    REM 进程详情
    if defined PID (
        echo.
        echo  [进程详情]
        tasklist /FI "PID eq !PID!" /FO LIST 2>nul | findstr /V "^$"
        echo.
        echo  [内存占用]
        tasklist /FI "PID eq !PID!" /FO TABLE 2>nul
    )

    REM 通过端口获取 PID
    for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":%APP_PORT% " ^| findstr "LISTENING"') do (
        echo  [端口占用 PID] %%a
    )

    echo.
    echo  [访问地址]
    echo   首页: http://localhost:%APP_PORT%
    echo   API文档: http://localhost:%APP_PORT%/doc.html
    echo   健康检查: http://localhost:%APP_PORT%/actuator/health
) else (
    echo  服务状态: 未运行
)
echo ============================================

REM 最近日志
if exist "%LOG_DIR%\sparkit.log" (
    echo.
    echo [INFO] 最近日志 (最后 10 行):
    echo ----------------------------------------
    powershell -Command "Get-Content '%LOG_DIR%\sparkit.log' -Tail 10"
    echo ----------------------------------------
)

endlocal