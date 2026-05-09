#Requires -Version 5.1
<#
.SYNOPSIS
    JeecgBoot Caddy 代理服务器启动脚本
.DESCRIPTION
    用于前后端联调，替代 nginx 的反向代理服务器
    统一入口: http://localhost:3000
    API代理: /jeecgboot -> http://localhost:8080/jeecg-boot
#>

[CmdletBinding()]
param(
    [switch]$ValidateOnly,
    [switch]$Stop,
    [string]$Config = "Caddyfile"
)

$ErrorActionPreference = "Stop"

# 路径配置
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$CaddyExe = Join-Path $ScriptDir "caddy.exe"
$CaddyConfig = Join-Path $ScriptDir $Config

function Write-Info($msg) { Write-Host "[INFO] $msg" -ForegroundColor Cyan }
function Write-Success($msg) { Write-Host "[OK] $msg" -ForegroundColor Green }
function Write-Warning($msg) { Write-Host "[WARN] $msg" -ForegroundColor Yellow }
function Write-Error($msg) { Write-Host "[ERROR] $msg" -ForegroundColor Red }

function Stop-CaddyServer {
    Write-Info "查找并停止已运行的 Caddy 进程..."
    $process = Get-Process -Name "caddy" -ErrorAction SilentlyContinue
    if ($process) {
        $process | Stop-Process -Force
        Write-Success "已停止 Caddy 进程"
        Start-Sleep -Seconds 1
    } else {
        Write-Warning "未找到运行中的 Caddy 进程"
    }
}

function Show-Banner {
    Write-Host ""
    Write-Host "==========================================" -ForegroundColor Cyan
    Write-Host "  JeecgBoot Caddy 代理服务器" -ForegroundColor Cyan
    Write-Host "==========================================" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "访问地址:"
    Write-Host "  - 主入口:   http://localhost:3000" -ForegroundColor Green
    Write-Host "  - 备选入口: http://localhost:8088" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "代理配置:"
    Write-Host "  - /jeecgboot/* -> http://localhost:8080/jeecg-boot/*"
    Write-Host "  - 前端静态资源  -> ../jeecgboot-vue3/dist"
    Write-Host ""
    Write-Host "==========================================" -ForegroundColor Cyan
    Write-Host ""
}

# 主逻辑
if ($Stop) {
    Stop-CaddyServer
    exit 0
}

Write-Host ""
Write-Info "Caddy 可执行文件: $CaddyExe"
Write-Info "配置文件: $CaddyConfig"
Write-Host ""

# 验证文件
if (-not (Test-Path $CaddyExe)) {
    Write-Error "未找到 caddy.exe: $CaddyExe"
    exit 1
}

if (-not (Test-Path $CaddyConfig)) {
    Write-Error "未找到配置文件: $CaddyConfig"
    exit 1
}

# 验证配置
Write-Info "验证 Caddy 配置..."
& $CaddyExe validate --config $CaddyConfig
if ($LASTEXITCODE -ne 0) {
    Write-Error "配置验证失败"
    exit 1
}
Write-Success "配置验证通过"

if ($ValidateOnly) {
    Write-Success "配置验证完成"
    exit 0
}

# 停止已运行实例
Stop-CaddyServer

# 显示信息
Show-Banner

Write-Info "启动 Caddy 服务器..."
Write-Host ""

# 启动 Caddy
try {
    & $CaddyExe run --config $CaddyConfig --watch
} catch {
    Write-Error "启动失败: $_"
    exit 1
}
