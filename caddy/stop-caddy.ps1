#Requires -Version 5.1
<#
.SYNOPSIS
    停止 JeecgBoot Caddy 代理服务器
#>

$ErrorActionPreference = "SilentlyContinue"

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "  停止 Caddy 代理服务器" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""

$process = Get-Process -Name "caddy" -ErrorAction SilentlyContinue
if ($process) {
    Write-Host "找到 Caddy 进程 (PID: $($process.Id))" -ForegroundColor Yellow
    $process | Stop-Process -Force
    Write-Host "[OK] Caddy 已停止" -ForegroundColor Green
} else {
    Write-Host "[INFO] 未找到运行中的 Caddy 进程" -ForegroundColor Cyan
}

Write-Host ""
