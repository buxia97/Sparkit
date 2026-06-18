@echo off
chcp 65001 >nul

REM ============================================
REM Sparkit - JVM 性能监控 (Windows)
REM 用法: jvm-monitor.bat
REM 说明: 查看 JVM 堆内存、GC、线程数等指标
REM ============================================

set "SCRIPT_DIR=%~dp0"
set "PROJECT_DIR=%SCRIPT_DIR%..\..\sparkit-server"
set "LOG_DIR=%PROJECT_DIR%\logs"
set "PID_FILE=%LOG_DIR%\sparkit.pid"
set "APP_PORT=8083"

echo ============================================
echo  Sparkit JVM 性能监控
echo ============================================

REM 获取 PID
set "PID="
if exist "%PID_FILE%" (
    set /p PID=<"%PID_FILE%"
)

if "%PID%"=="" (
    for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":%APP_PORT% " ^| findstr "LISTENING"') do (
        set "PID=%%a"
    )
)

if "%PID%"=="" (
    echo [ERROR] 未找到运行的 Sparkit 服务
    echo [INFO] 请先启动服务: start.bat
    exit /b 1
)

echo [INFO] 服务 PID: %PID%
echo.

REM 检查 Java 环境
if not defined JAVA_HOME (
    echo [ERROR] JAVA_HOME 未设置
    exit /b 1
)

set "JPS=%JAVA_HOME%\bin\jps.exe"
set "JSTAT=%JAVA_HOME%\bin\jstat.exe"
set "JINFO=%JAVA_HOME%\bin\jinfo.exe"

echo ============================================
echo  [1] JVM 进程信息
echo ============================================
if exist "%JPS%" (
    "%JPS%" -l -v | findstr "%PID%"
) else (
    echo [WARN] jps 工具不可用
)

echo.
echo ============================================
echo  [2] 堆内存使用情况
echo ============================================
if exist "%JSTAT%" (
    "%JSTAT%" -gc %PID%
    echo.
    echo  [GC 统计]
    "%JSTAT%" -gcutil %PID% 1 1
) else (
    echo [WARN] jstat 工具不可用
)

echo.
echo ============================================
echo  [3] 线程信息
echo ============================================
if exist "%JINFO%" (
    "%JINFO%" -sysprops %PID% 2>nul | findstr "java.vm.name java.version"
) else (
    echo [WARN] jinfo 工具不可用
)

echo.
echo ============================================
echo  [4] 系统进程信息
echo ============================================
tasklist /FI "PID eq %PID%" /FO LIST 2>nul | findstr /V "^$"

echo.
echo [INFO] 监控完成
echo [INFO] 也可通过 Actuator 端点获取详情: http://localhost:%APP_PORT%/actuator/metrics