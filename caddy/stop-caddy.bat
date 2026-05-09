@echo off
chcp 65001 >nul
echo ==========================================
echo 停止 Caddy 代理服务器
echo ==========================================
echo.

echo [1/2] 查找 Caddy 进程...
tasklist | findstr /I "caddy.exe"
if errorlevel 1 (
    echo [信息] 未找到运行中的 Caddy 进程
) else (
    echo [信息] 找到 Caddy 进程
echo.
    echo [2/2] 正在停止 Caddy...
    taskkill /F /IM caddy.exe
    if errorlevel 1 (
        echo [错误] 停止 Caddy 失败
    ) else (
        echo [✓] Caddy 已成功停止
    )
)

echo.
pause
