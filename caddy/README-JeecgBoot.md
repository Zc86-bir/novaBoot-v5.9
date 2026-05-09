# JeecgBoot Caddy 联调配置

本配置使用 [Caddy](https://caddyserver.com/) 替代 nginx 作为前后端联调反向代理服务器。

## 为什么选择 Caddy？

- **自动 HTTPS**: 内置 HTTPS，自动签发和续期证书
- **配置简单**: 使用 Caddyfile 配置，语法简洁直观
- **高性能**: 使用 Go 编写，性能优异
- **实时重载**: 支持配置热重载，无需重启
- **现代协议**: 原生支持 HTTP/2, HTTP/3, WebSocket

## 文件说明

| 文件 | 说明 |
|------|------|
| `caddy.exe` | Caddy 可执行文件 (v2.x) |
| `Caddyfile` | 主配置文件 |
| `start-caddy.bat` | Windows CMD 启动脚本 |
| `start-caddy.ps1` | Windows PowerShell 启动脚本（推荐） |
| `stop-caddy.bat` | Windows CMD 停止脚本 |
| `stop-caddy.ps1` | Windows PowerShell 停止脚本 |
| `caddy-access.log` | 访问日志（运行时生成） |

## 代理配置

```
┌─────────────────┐         ┌─────────────┐         ┌─────────────────┐
│   浏览器访问    │────────▶│  Caddy      │────────▶│   后端服务      │
│ localhost:3000  │         │  :3000      │         │ localhost:8080 │
└─────────────────┘         └─────────────┘         └─────────────────┘
                                   │
                                   ▼
                            ┌─────────────┐
                            │ 前端静态资源 │
                            │ /dist      │
                            └─────────────┘
```

| 路径 | 代理目标 | 说明 |
|------|----------|------|
| `/` | `../jeecgboot-vue3/dist` | 前端静态资源 |
| `/jeecgboot/*` | `http://localhost:8080/jeecg-boot/*` | API 请求 |
| `/upload/*` | `http://localhost:3300/upload` | 上传服务 |

## 快速开始

### 1. 启动后端服务

```powershell
cd ../jeecg-boot/jeecg-module-system/jeecg-system-start
mvn spring-boot:run
```

后端默认运行在 http://localhost:8080/jeecg-boot

### 2. 构建前端（生产模式）

```powershell
cd ../jeecgboot-vue3
pnpm build
```

### 3. 启动 Caddy 代理

**使用 PowerShell（推荐）:**
```powershell
.\start-caddy.ps1
```

**或使用 CMD:**
```cmd
start-caddy.bat
```

### 4. 访问应用

打开浏览器访问：http://localhost:3000

### 5. 停止 Caddy

```powershell
.\stop-caddy.ps1
```

或

```cmd
stop-caddy.bat
```

## 前端开发模式（热更新）

如果需要使用 vite 开发服务器的热更新功能，修改前端配置：

### 方法1: 使用 vite 代理（推荐开发时用）

编辑 `../jeecgboot-vue3/.env.development`:

```bash
# 保持默认配置，使用 vite 内置代理
VITE_PROXY = [["/jeecgboot","http://localhost:8080/jeecg-boot"]]
VITE_GLOB_DOMAIN_URL=http://localhost:8080/jeecg-boot
VITE_GLOB_API_URL=/jeecgboot
```

启动 vite 开发服务器:
```bash
cd ../jeecgboot-vue3
pnpm dev
```

访问: http://localhost:3100

### 方法2: 使用 Caddy + vite（联调测试）

如果需要统一入口测试：

1. 修改 `.env.development`:
```bash
# 代理指向 caddy
VITE_PROXY = [["/jeecgboot","http://localhost:3000/jeecgboot"]]
VITE_GLOB_DOMAIN_URL=http://localhost:3000/jeecgboot
VITE_GLOB_API_URL=/jeecgboot
```

2. 修改 Caddyfile，添加 vite 开发服务器代理:

```caddyfile
localhost:3000 {
    # 开发模式：代理到 vite 开发服务器
    reverse_proxy localhost:3101 {
        header_up Host {upstream_hostport}
    }

    # API 保持原配置
    handle /jeecgboot/* {
        rewrite * /jeecg-boot{uri}
        reverse_proxy localhost:8080
    }
}
```

## 配置热重载

Caddy 支持配置热重载，修改 `Caddyfile` 后执行：

```powershell
.\caddy.exe reload --config Caddyfile
```

或在启动时使用 `--watch` 参数（已包含在启动脚本中）。

## 常见问题

### 1. 端口冲突

如果 3000 端口被占用，可以：
- 使用备选端口 8088
- 修改 `Caddyfile` 中的端口配置

### 2. 后端地址变更

如果后端不在 localhost:8080，修改 `Caddyfile`:

```caddyfile
reverse_proxy 你的后端地址:端口 {
    ...
}
```

### 3. 前端资源路径错误

确保已构建前端:
```bash
cd ../jeecgboot-vue3
pnpm build
```

或修改 `Caddyfile` 中的 root 路径:
```caddyfile
root * 你的前端dist目录路径
```

### 4. API 跨域问题

Caddyfile 中已配置 CORS 响应头。如果仍有问题，检查：
1. 后端是否正确响应
2. 请求路径是否正确
3. Cookie 是否携带

## Caddy 命令参考

```bash
# 验证配置
caddy.exe validate --config Caddyfile

# 启动服务器（前台运行）
caddy.exe run --config Caddyfile

# 启动服务器（后台运行 - Linux/Mac）
caddy.exe start --config Caddyfile

# 重载配置
caddy.exe reload --config Caddyfile

# 停止服务器
caddy.exe stop

# 格式化配置
caddy.exe fmt --overwrite Caddyfile

# 查看版本
caddy.exe version
```

## 进阶配置

### 启用 HTTPS（本地开发）

编辑 Caddyfile，删除 `auto_https off` 行，添加域名:

```caddyfile
localhost {
    tls internal  # 使用内部 CA 签发自签名证书
    ...
}
```

访问 https://localhost

### 启用 HTTP/3

```caddyfile
{
    servers {
        protocol {
            experimental_http3
        }
    }
}
```

### 负载均衡（多后端）

```caddyfile
reverse_proxy backend1:8080 backend2:8080 {
    lb_policy round_robin
}
```

## 参考文档

- [Caddy 官方文档](https://caddyserver.com/docs/)
- [Caddyfile 语法](https://caddyserver.com/docs/caddyfile)
- [反向代理教程](https://caddyserver.com/docs/quick-starts/reverse-proxy)

## 与 nginx 对比

| 功能 | nginx | Caddy |
|------|-------|-------|
| 配置语法 | 较复杂 | 简洁直观 |
| HTTPS | 手动配置 | 自动（内置） |
| 热重载 | 需要信号 | 原生支持 |
| HTTP/3 | 实验性 | 原生支持 |
| 日志 | 需配置格式 | JSON 默认 |
| 性能 | 优秀 | 优秀 |

## 许可证

Caddy 使用 Apache 2.0 许可证。
