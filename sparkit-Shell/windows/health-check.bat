@echo off
chcp 65001 >nul

REM ============================================
REM Sparkit - 健康检查 (Windows)
REM 用法: health-check.bat
REM 说明: 调用 Actuator /health 端点，返回服务健康状态
REM ============================================

set "APP_PORT=8083"
set "HEALTH_URL=http://localhost:%APP_PORT%/actuator/health"

echo ============================================
echo  Sparkit 健康检查
echo ============================================
echo  检查地址: %HEALTH_URL%
echo ============================================

REM 使用 curl 检查
where curl >nul 2>&1
if %errorlevel% NEQ 0 (
    REM 尝试使用 PowerShell
    powershell -Command "try { $response = Invoke-WebRequest -Uri '%HEALTH_URL%' -UseBasicParsing -TimeoutSec 5; Write-Host $response.Content; if ($response.StatusCode -eq 200) { Write-Host '[SUCCESS] 服务健康' -ForegroundColor Green } else { Write-Host '[WARN] 服务状态异常' -ForegroundColor Yellow } } catch { Write-Host '[ERROR] 无法连接到服务' -ForegroundColor Red; Write-Host $_ }"
    exit /b
)

curl -s "%HEALTH_URL%" 2>nul
if %errorlevel% EQU 0 (
    echo.
    echo [SUCCESS] 服务健康
) else (
    echo [ERROR] 无法连接到服务，请确认服务已启动
    echo [INFO] 尝试启动服务: start.bat
)