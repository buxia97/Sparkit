@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

REM ============================================
REM Sparkit - 启动服务 (Windows)
REM 用法: start.bat [profile] [jvm_opts]
REM 示例: start.bat dev
REM       start.bat prod "-Xms512m -Xmx1024m"
REM ============================================

set "SCRIPT_DIR=%~dp0"
set "PROJECT_DIR=%SCRIPT_DIR%..\..\sparkit-server"
set "JAR_NAME=sparkit-start-1.0.0-SNAPSHOT.jar"
set "JAR_DIR=%PROJECT_DIR%\sparkit-start\target"

REM 默认 profile
set "PROFILE=dev"
if not "%~1"=="" set "PROFILE=%~1"

REM 默认 JVM 参数
set "JVM_OPTS=-Xms256m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
if not "%~2"=="" set "JVM_OPTS=%~2"

REM 日志目录
set "LOG_DIR=%PROJECT_DIR%\logs"
if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"

REM PID 文件
set "PID_FILE=%LOG_DIR%\sparkit.pid"
set "APP_PORT=8083"

echo ============================================
echo  Sparkit 服务启动脚本
echo ============================================
echo  项目目录: %PROJECT_DIR%
echo  启动 Profile: %PROFILE%
echo  JVM 参数: %JVM_OPTS%
echo ============================================

REM 检查是否已运行
if exist "%PID_FILE%" (
    set /p PID=<"%PID_FILE%"
    tasklist /FI "PID eq !PID!" 2>nul | find /I "!PID!" >nul
    if !errorlevel!==0 (
        echo [WARN] 服务已在运行中 (PID: !PID!)
        echo [WARN] 如需重启请执行 restart.bat
        exit /b 0
    ) else (
        del /f "%PID_FILE%" >nul 2>&1
    )
)

REM 检查端口
netstat -ano | findstr ":%APP_PORT% " | findstr "LISTENING" >nul
if !errorlevel!==0 (
    echo [WARN] 端口 %APP_PORT% 已被占用，请检查
    netstat -ano | findstr ":%APP_PORT% " | findstr "LISTENING"
    exit /b 1
)

REM 检查 JAR 文件
if not exist "%JAR_DIR%\%JAR_NAME%" (
    echo [ERROR] JAR 文件不存在: %JAR_DIR%\%JAR_NAME%
    echo [INFO] 请先执行: cd %PROJECT_DIR% ^&^& mvn clean install -DskipTests
    exit /b 1
)

REM 检查 Java
if not defined JAVA_HOME (
    echo [ERROR] JAVA_HOME 未设置，请先配置 Java 环境变量
    exit /b 1
)

set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
if not exist "%JAVA_EXE%" (
    echo [ERROR] Java 未找到: %JAVA_EXE%
    exit /b 1
)

echo [INFO] Java 版本:
"%JAVA_EXE%" -version 2>&1

REM 启动服务
echo [INFO] 正在启动 Sparkit 服务...
start /B "" "%JAVA_EXE%" %JVM_OPTS% ^
    -Dspring.profiles.active=%PROFILE% ^
    -Dserver.port=%APP_PORT% ^
    -Dfile.encoding=UTF-8 ^
    -jar "%JAR_DIR%\%JAR_NAME%" ^
    > "%LOG_DIR%\sparkit.log" 2>&1

REM 获取 PID
timeout /t 2 /nobreak >nul
for /f "tokens=2" %%a in ('tasklist /FI "IMAGENAME eq java.exe" /FO TABLE /NH 2^>nul') do (
    set "PID=%%a"
)
if defined PID (
    echo !PID! > "%PID_FILE%"
    echo [INFO] 服务已启动，PID: !PID!
) else (
    echo [INFO] 服务正在启动中，请稍候...
)

REM 等待服务就绪
echo [INFO] 等待服务就绪...
set "RETRY=0"
:wait_loop
timeout /t 2 /nobreak >nul
set /a RETRY+=1

REM 检查端口
netstat -ano | findstr ":%APP_PORT% " | findstr "LISTENING" >nul
if !errorlevel!==0 (
    echo [SUCCESS] 服务启动成功！端口 %APP_PORT% 已监听
    echo [INFO] 访问地址: http://localhost:%APP_PORT%
    echo [INFO] API 文档: http://localhost:%APP_PORT%/doc.html
    echo [INFO] 日志文件: %LOG_DIR%\sparkit.log
    goto :end
)

if !RETRY! LSS 30 goto :wait_loop

echo [ERROR] 服务启动超时，请检查日志: %LOG_DIR%\sparkit.log
exit /b 1

:end
endlocal