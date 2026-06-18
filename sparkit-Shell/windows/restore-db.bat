@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

REM ============================================
REM Sparkit - 数据库恢复 (Windows)
REM 用法: restore-db.bat [backup_file]
REM 示例: restore-db.bat sparkit_backup_20260101_120000.sql
REM       restore-db.bat (交互式选择备份文件)
REM ============================================

set "SCRIPT_DIR=%~dp0"
set "PROJECT_DIR=%SCRIPT_DIR%..\..\sparkit-server"
set "BACKUP_DIR=%PROJECT_DIR%\backup"

REM 数据库配置
set "DB_HOST=localhost"
set "DB_PORT=3306"
set "DB_NAME=sparkit"
set "DB_USER=root"
set "DB_PASS=root"

echo ============================================
echo  Sparkit 数据库恢复
echo ============================================
echo  数据库: %DB_NAME%
echo ============================================

REM 确定备份文件
set "BACKUP_FILE=%~1"

if "%BACKUP_FILE%"=="" (
    echo [INFO] 可用备份文件:
    echo.
    set "IDX=0"
    for %%f in ("%BACKUP_DIR%\sparkit_backup_*.sql") do (
        set /a IDX+=1
        echo   [!IDX!] %%~nxf
        set "FILE_!IDX!=%%f"
    )
    for %%f in ("%BACKUP_DIR%\sparkit_backup_*.sql.zip") do (
        set /a IDX+=1
        echo   [!IDX!] %%~nxf
        set "FILE_!IDX!=%%f"
    )

    if !IDX!==0 (
        echo [ERROR] 未找到备份文件，请先执行 backup-db.bat
        exit /b 1
    )

    echo.
    set /p "CHOICE=请选择要恢复的备份文件编号 [1-!IDX!]: "
    if "!CHOICE!"=="" (
        echo [ERROR] 未选择文件
        exit /b 1
    )
    set "BACKUP_FILE=!FILE_%CHOICE%!"
)

if not exist "%BACKUP_FILE%" (
    echo [ERROR] 备份文件不存在: %BACKUP_FILE%
    exit /b 1
)

echo.
echo [INFO] 备份文件: %BACKUP_FILE%
echo [WARN] 恢复操作将覆盖现有数据库数据！
echo.

set /p "CONFIRM=确认恢复？请输入 yes 继续: "
if /I not "%CONFIRM%"=="yes" (
    echo [INFO] 已取消恢复操作
    exit /b 0
)

REM 如果是 zip 文件，先解压
set "SQL_FILE=%BACKUP_FILE%"
echo %BACKUP_FILE% | findstr /I ".zip" >nul
if %errorlevel% EQU 0 (
    echo [INFO] 正在解压备份文件...
    powershell -Command "Expand-Archive -Path '%BACKUP_FILE%' -DestinationPath '%BACKUP_DIR%' -Force" 2>nul
    set "SQL_FILE=%BACKUP_FILE:~0,-4%"
    if not exist "!SQL_FILE!" (
        echo [ERROR] 解压失败
        exit /b 1
    )
)

REM 检查 mysql 客户端
set "MYSQL=mysql"
where mysql >nul 2>&1
if %errorlevel% NEQ 0 (
    if defined MYSQL_HOME (
        set "MYSQL=%MYSQL_HOME%\bin\mysql.exe"
    )
)

REM 执行恢复
echo [INFO] 正在恢复数据库...
"%MYSQL%" -h%DB_HOST% -P%DB_PORT% -u%DB_USER% -p%DB_PASS% ^
    --default-character-set=utf8mb4 ^
    %DB_NAME% < "!SQL_FILE!"

if %errorlevel% NEQ 0 (
    echo [ERROR] 数据库恢复失败！
    exit /b 1
)

echo [SUCCESS] 数据库恢复成功！
echo [INFO] 建议重启服务以刷新缓存: restart.bat

endlocal