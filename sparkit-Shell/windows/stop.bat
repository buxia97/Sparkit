@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

REM ============================================
REM Sparkit - 停止服务 (Windows)
REM 用法: stop.bat
REM 说明: 优雅停止（先 SIGTERM，超时后强杀）
REM ============================================

set "SCRIPT_DIR=%~dp0"
set "PROJECT_DIR=%SCRIPT_DIR%..\..\sparkit-server"
set "LOG_DIR=%PROJECT_DIR%\logs"
set "PID_FILE=%LOG_DIR%\sparkit.pid"
set "APP_PORT=8083"

echo ============================================
echo  Sparkit 服务停止脚本
echo ============================================

REM 方式1: 通过 PID 文件停止
if exist "%PID_FILE%" (
    set /p PID=<"%PID_FILE%"
    echo [INFO] 找到 PID 文件，PID: !PID!

    tasklist /FI "PID eq !PID!" 2>nul | find /I "!PID!" >nul
    if !errorlevel!==0 (
        echo [INFO] 正在优雅停止服务 (PID: !PID!)...
        taskkill /PID !PID! >nul 2>&1

        REM 等待进程退出
        set "WAIT=0"
        :wait_pid
        timeout /t 1 /nobreak >nul
        set /a WAIT+=1
        tasklist /FI "PID eq !PID!" 2>nul | find /I "!PID!" >nul
        if !errorlevel! NEQ 0 (
            echo [SUCCESS] 服务已停止 (PID: !PID!)
            del /f "%PID_FILE%" >nul 2>&1
            goto :done
        )
        if !WAIT! LSS 15 goto :wait_pid

        REM 超时，强杀
        echo [WARN] 优雅停止超时，执行强制终止...
        taskkill /F /PID !PID! >nul 2>&1
        timeout /t 2 /nobreak >nul
        tasklist /FI "PID eq !PID!" 2>nul | find /I "!PID!" >nul
        if !errorlevel! EQU 0 (
            echo [ERROR] 强制终止失败，PID: !PID!
        ) else (
            echo [SUCCESS] 服务已强制终止 (PID: !PID!)
        )
        del /f "%PID_FILE%" >nul 2>&1
        goto :done
    ) else (
        echo [WARN] PID !PID! 对应的进程不存在，清理 PID 文件
        del /f "%PID_FILE%" >nul 2>&1
    )
)

REM 方式2: 通过端口查找并停止
echo [INFO] 通过端口 %APP_PORT% 查找进程...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":%APP_PORT% " ^| findstr "LISTENING"') do (
    set "PID=%%a"
    echo [INFO] 找到监听端口 %APP_PORT% 的进程，PID: !PID!
    echo [INFO] 正在停止服务...
    taskkill /PID !PID! >nul 2>&1

    REM 等待进程退出
    set "WAIT=0"
    :wait_port
    timeout /t 1 /nobreak >nul
    set /a WAIT+=1
    tasklist /FI "PID eq !PID!" 2>nul | find /I "!PID!" >nul
    if !errorlevel! NEQ 0 (
        echo [SUCCESS] 服务已停止 (PID: !PID!)
        goto :done
    )
    if !WAIT! LSS 15 goto :wait_port

    REM 超时，强杀
    echo [WARN] 优雅停止超时，执行强制终止...
    taskkill /F /PID !PID! >nul 2>&1
    echo [SUCCESS] 服务已强制终止 (PID: !PID!)
    goto :done
)

echo [INFO] 未找到正在运行的 Sparkit 服务

:done
echo [INFO] 停止完成
endlocal