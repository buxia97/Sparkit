@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

REM ============================================
REM Sparkit - 数据库备份 (Windows)
REM 用法: backup-db.bat
REM 说明: 执行 MySQL 全量备份并压缩归档
REM ============================================

set "SCRIPT_DIR=%~dp0"
set "PROJECT_DIR=%SCRIPT_DIR%..\..\sparkit-server"
set "BACKUP_DIR=%PROJECT_DIR%\backup"
set "LOG_DIR=%PROJECT_DIR%\logs"

REM 数据库配置
set "DB_HOST=localhost"
set "DB_PORT=3306"
set "DB_NAME=sparkit"
set "DB_USER=root"
set "DB_PASS=root"

REM 备份目录
if not exist "%BACKUP_DIR%" mkdir "%BACKUP_DIR%"

REM 备份文件名
set "TIMESTAMP=%date:~0,4%%date:~5,2%%date:~8,2%_%time:~0,2%%time:~3,2%%time:~6,2%"
set "TIMESTAMP=%TIMESTAMP: =0%"
set "BACKUP_FILE=%BACKUP_DIR%\sparkit_backup_%TIMESTAMP%.sql"
set "ZIP_FILE=%BACKUP_FILE%.zip"

echo ============================================
echo  Sparkit 数据库备份
echo ============================================
echo  数据库: %DB_NAME%
echo  备份目录: %BACKUP_DIR%
echo  备份文件: %BACKUP_FILE%
echo ============================================

REM 检查 mysqldump
set "MYSQLDUMP=mysqldump"
where mysqldump >nul 2>&1
if %errorlevel% NEQ 0 (
    if defined MYSQL_HOME (
        set "MYSQLDUMP=%MYSQL_HOME%\bin\mysqldump.exe"
    )
)

REM 执行备份
echo [INFO] 正在备份数据库...
"%MYSQLDUMP%" -h%DB_HOST% -P%DB_PORT% -u%DB_USER% -p%DB_PASS% ^
    --default-character-set=utf8mb4 ^
    --single-transaction ^
    --routines ^
    --triggers ^
    --events ^
    --hex-blob ^
    %DB_NAME% > "%BACKUP_FILE%" 2>"%LOG_DIR%\backup_error.log"

if %errorlevel% NEQ 0 (
    echo [ERROR] 数据库备份失败！请检查错误日志: %LOG_DIR%\backup_error.log
    type "%LOG_DIR%\backup_error.log"
    exit /b 1
)

echo [SUCCESS] 备份完成: %BACKUP_FILE%

REM 压缩备份文件
echo [INFO] 正在压缩备份文件...
powershell -Command "Compress-Archive -Path '%BACKUP_FILE%' -DestinationPath '%ZIP_FILE%' -Force" 2>nul
if %errorlevel% EQU 0 (
    del /f "%BACKUP_FILE%" >nul 2>&1
    for %%a in ("%ZIP_FILE%") do set "SIZE=%%~za"
    set /a "SIZE_MB=!SIZE!/1048576"
    echo [SUCCESS] 已压缩: %ZIP_FILE% (!SIZE_MB! MB)
) else (
    echo [WARN] 压缩失败，保留原始文件
)

REM 清理旧备份（保留最近 30 天）
echo [INFO] 清理 30 天前的备份...
forfiles /p "%BACKUP_DIR%" /m "sparkit_backup_*.zip" /d -30 /c "cmd /c del @file" 2>nul

echo.
echo [INFO] 备份完成
endlocal