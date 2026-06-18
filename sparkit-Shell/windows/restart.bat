@echo off
chcp 65001 >nul

REM ============================================
REM Sparkit - 重启服务 (Windows)
REM 用法: restart.bat [profile]
REM ============================================

set "SCRIPT_DIR=%~dp0"

echo ============================================
echo  Sparkit 服务重启
echo ============================================

echo [STEP 1/2] 正在停止服务...
call "%SCRIPT_DIR%stop.bat"
echo.

echo [STEP 2/2] 正在启动服务...
call "%SCRIPT_DIR%start.bat" %*

echo.
echo [INFO] 重启完成