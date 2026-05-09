@echo off
chcp 65001 >nul
cd /d "%~dp0"

echo ==========================================
echo JeecgBoot Caddy 代理服务器
echo ==========================================
echo.

set CADDY_PATH=%CD%\caddy.exe
set CADDY_CONFIG=%CD%\Caddyfile

echo [1/4] 检查环境...
if not exist "%CADDY_PATH%" (
    echo [错误] 未找到 caddy.exe
    pause
    exit /b 1
)

echo [2/4] 验证配置...
"%CADDY_PATH%" validate --config "%CADDY_CONFIG%" >nul 2>&1
if errorlevel 1 (
    echo [错误] Caddy 配置验证失败
    "%CADDY_PATH%" validate --config "%CADDY_CONFIG%"
    pause
    exit /b 1
)
echo [OK] 配置验证通过

echo [3/4] 检查端口...
netstat -an | findstr ":3000" | findstr "LISTENING" >nul
if not errorlevel 1 (
    echo [警告] 端口 3000 已被占用，尝试停止...
    taskkill /F /IM caddy.exe >nul 2>&1
    timeout /t 2 /nobreak >nul
)

echo [4/4] 启动 Caddy...
echo.
echo ==========================================
echo Caddy 代理已启动！
echo.
echo 访问地址: http://localhost:3000
echo.
echo 代理配置:
echo   - /jeecgboot/* -^> http://localhost:8080/jeecg-boot/*
echo   - 前端静态资源 -^> ../jeecgboot-vue3/dist
echo.
echo 注意: 请确保后端服务已启动（端口 8080）
echo ==========================================
echo.
echo 按 Ctrl+C 停止服务器
echo.

"%CADDY_PATH%" run --config "%CADDY_CONFIG%" --watch 2>&1
