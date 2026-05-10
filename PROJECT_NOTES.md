# NovaBoot v5.9 项目笔记

> 记录项目启动依赖、接口路径、常见报错处理及后续开发规划

---

## 一、启动依赖

### 1.1 环境要求

| 组件 | 版本要求 | 说明 |
|------|---------|------|
| JDK | 17/21/24/25 | 推荐 JDK 21 LTS |
| Node.js | 20.19+ 或 22.12+ | 必须 20+，Vite 6 要求 |
| pnpm | 9+ | 包管理器 |
| MySQL | 5.7+ | 默认数据库 |
| Redis | 5.0+ | 缓存 + Session |

### 1.2 端口占用清单

| 服务 | 端口 | 用途 |
|------|------|------|
| 后端服务 | 8080 | Spring Boot |
| 前端开发 | 3100 | Vite Dev Server |
| MySQL | 3306 / 13306 (Docker) | 数据存储 |
| Redis | 6379 | 缓存 |
| Nacos | 8848 | 微服务注册中心（可选） |
| Sentinel | 9000 | 限流熔断（可选） |
| XXL-Job | 9080 | 分布式任务（可选） |

### 1.3 启动顺序

```bash
# 1. 启动数据库（Docker 或本地）
docker-compose up -d mysql redis

# 2. 启动后端
cd jeecg-boot/jeecg-module-system/jeecg-system-start
mvn spring-boot:run

# 3. 启动前端
cd jeecgboot-vue3
pnpm install
pnpm dev

# 4. 访问系统
# 后台: http://localhost:3100
# 默认账号: admin / 123456
```

### 1.4 数据库初始化

```sql
-- 创建数据库
CREATE DATABASE nova_boot 
  DEFAULT CHARSET utf8mb4 
  COLLATE utf8mb4_unicode_ci;

-- 执行基础脚本
db/jeecgboot-mysql-5.7.sql

-- Flyway 自动迁移
-- V3.9.3_1__usstock_more.sql  (美股数据表)
-- V3.9.3_2__crypto_data.sql   (加密货币数据表)
```

### 1.5 配置文件清单

| 配置项 | 文件路径 | 关键配置 |
|--------|---------|---------|
| 后端主配置 | `jeecg-boot/.../application-dev.yml` | 数据库、Redis |
| 后端环境 | `jeecg-boot/.../application.yml` | 激活 dev 环境 |
| 前端环境 | `jeecgboot-vue3/.env.development` | API 地址、端口 |
| 代理配置 | `jeecgboot-vue3/vite.config.ts` | 跨域代理 |
| 定时任务 | `QuartzJobConfig.java` | Cron 表达式 |

---

## 二、接口路径汇总

### 2.1 新增接口（加密货币 + 美股）

#### 加密货币模块

| 接口地址 | 方法 | 入参 | 出参 | 说明 |
|---------|------|------|------|------|
| `/test/crypto/list` | GET | 无 | `Result<List<CryptoVO>>` | 数据库列表 |
| `/test/crypto/realtime` | GET | 无 | `Result<List<Map>>` | CoinGecko 实时数据 |
| `/test/crypto/sync` | POST | 无 | `Result<String>` | 手动同步 |

**CryptoVO 字段:**
```java
String symbol;           // 币种代码
String name;             // 币种名称
BigDecimal price;        // 当前价格(USD)
BigDecimal changePct24h; // 24小时涨跌幅%
BigDecimal changePct7d;  // 7天涨跌幅%
BigDecimal volume24h;    // 24小时成交量
BigDecimal marketCap;    // 市值
Integer marketCapRank;   // 市值排名
```

#### 美股模块

| 接口地址 | 方法 | 入参 | 出参 | 说明 |
|---------|------|------|------|------|
| `/test/usstock/list` | GET | 无 | `Result<List<UsStockVO>>` | 数据库列表 |
| `/test/usstock/realtime` | GET | 无 | `Result<List<Map>>` | 实时模拟数据 |
| `/test/usstock/sync` | POST | 无 | `Result<String>` | 手动同步 |

**UsStockVO 字段:**
```java
String symbol;           // 股票代码
String name;             // 股票名称
String sector;           // 行业分类
BigDecimal closePrice;   // 收盘价
BigDecimal prevClose;    // 前收盘价
BigDecimal change;       // 涨跌额
BigDecimal changePct;    // 涨跌幅%
Long volume;             // 成交量
String marketCap;        // 市值
String tradeDate;        // 交易日期
```

### 2.2 系统核心接口

| 接口地址 | 方法 | 说明 |
|---------|------|------|
| `/sys/login` | POST | 用户登录 |
| `/sys/logout` | POST | 用户登出 |
| `/sys/user/info` | GET | 获取当前用户信息 |
| `/sys/permission/getUserPermissionByToken` | GET | 获取用户权限 |
| `/sys/dict/getDictItems/{dictCode}` | GET | 获取字典项 |
| `/sys/annountCement/list` | GET | 系统公告列表 |

### 2.3 Online 低代码接口

| 接口地址前缀 | 说明 |
|-------------|------|
| `/online/cgform/api/getColumns/` | 获取表单配置 |
| `/online/cgform/api/getData/` | 获取表单数据 |
| `/online/cgform/api/addAll/` | 新增数据 |
| `/online/cgform/api/editAll/` | 编辑数据 |
| `/online/cgform/api/delAll/` | 删除数据 |
| `/online/cgform/api/exportXls/` | 导出 Excel |
| `/online/cgform/api/importXls/` | 导入 Excel |

### 2.4 AI 应用平台接口

| 接口地址 | 方法 | 说明 |
|---------|------|------|
| `/airag/app/list` | GET | AI 应用列表 |
| `/airag/app/chat` | POST | AI 对话 |
| `/airag/knowledge/list` | GET | 知识库列表 |
| `/airag/knowledge/upload` | POST | 上传文档 |
| `/airag/model/list` | GET | 模型列表 |
| `/airag/mcp/list` | GET | MCP 插件列表 |

---

## 三、常见报错处理

### 3.1 后端报错

#### 报错 1：端口 8080 被占用
```
Web server failed to start. Port 8080 was already in use.
```

**解决方式:**
```bash
# Windows PowerShell
Get-Process -Id (Get-NetTCPConnection -LocalPort 8080).OwningProcess
Stop-Process -Id <PID> -Force

# 或修改端口
# application-dev.yml
server:
  port: 8081  # 修改为其他端口
```

#### 报错 2：数据库连接失败
```
Communications link failure: com.mysql.cj.jdbc.exceptions.CommunicationsException
```

**解决方式:**
1. 检查 MySQL 服务是否启动
2. 检查连接配置（host、port、用户名、密码）
3. 检查数据库是否存在
4. 检查防火墙设置

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/nova_boot?...
    username: root
    password: root
```

#### 报错 3：Redis 连接失败
```
Unable to connect to Redis
```

**解决方式:**
```bash
# 检查 Redis 服务
redis-cli ping

# 或启动 Redis
redis-server
```

#### 报错 4：Flyway 迁移失败
```
FlywayException: Validate failed: Migration checksum mismatch
```

**解决方式:**
```sql
-- 清理 Flyway 元数据表（重新执行迁移）
DELETE FROM flyway_schema_history WHERE success = 0;
-- 或删除数据库重建
DROP DATABASE nova_boot;
CREATE DATABASE nova_boot DEFAULT CHARSET utf8mb4;
```

#### 报错 5：Quartz 表不存在
```
Table 'nova_boot.QRTZ_TRIGGERS' doesn't exist
```

**解决方式:**
```sql
-- 执行 Quartz 初始化 SQL
db/tables_nacos.sql
-- 或检查 quartz.properties 配置
```

#### 报错 6：Maven 编译失败
```
Could not find artifact ...
```

**解决方式:**
```bash
# 清理并重新编译
mvn clean install -DskipTests

# 如仍失败，检查网络连接（Maven 仓库）
# 或使用阿里云镜像
```

### 3.2 前端报错

#### 报错 1：pnpm install 失败
```
ERR_PNPM_FETCH_404
```

**解决方式:**
```bash
# 清除缓存
pnpm store prune
pnpm cache clean

# 重新安装
pnpm install

# 或使用淘宝镜像
pnpm config set registry https://registry.npmmirror.com
```

#### 报错 2：Vite 启动失败
```
Port 3100 is already in use
```

**解决方式:**
```bash
# Windows 查找并结束进程
netstat -ano | findstr 3100
taskkill /PID <PID> /F

# 或修改端口
# .env.development
VITE_PORT=3101
```

#### 报错 3：代理配置错误
```
Proxy error: Could not proxy request
```

**解决方式:**
```typescript
// vite.config.ts
proxy: {
  '/jeecg-boot': {
    target: 'http://localhost:8080',
    changeOrigin: true,
    rewrite: (path) => path.replace(/^\/jeecg-boot/, '')
  }
}
```

#### 报错 4：组件无法解析
```
Failed to resolve component
```

**解决方式:**
1. 检查组件路径是否正确
2. 检查组件是否已注册（全局或局部）
3. 检查组件名是否一致（大小写敏感）

#### 报错 5：TypeScript 类型错误
```
TS2345: Argument of type 'xxx' is not assignable to parameter of type 'yyy'
```

**解决方式:**
```typescript
// 添加类型断言
const data = res.result as CryptoVO[]

// 或使用 any（不推荐）
const data: any = res.result
```

### 3.3 业务报错

#### 报错 1：404 页面不存在
```
抱歉，您访问的页面不存在
```

**解决方式:**
```typescript
// 检查路由配置
// 确保路由已添加到 staticRouter.ts 或对应模块路由文件
{
  path: '/report/crypto',
  component: () => import('/@/views/report/crypto/bigscreen/index.vue')
}
```

#### 报错 2：权限不足
```
抱歉，您没有权限访问该页面
```

**解决方式:**
1. 检查用户角色权限
2. 检查菜单权限配置
3. 检查接口权限注解（`@RequiresPermissions`）

#### 报错 3：数据不显示
```
表格/图表无数据
```

**解决方式:**
```bash
# 1. 检查后端接口是否正常返回
# 2. 检查前端 API 调用路径是否正确
# 3. 检查数据库是否有数据
# 4. 检查浏览器 Network 面板

# 快速测试接口
curl http://localhost:8080/test/crypto/list
```

---

## 四、后续开发规划

### 4.1 优先级排序

| 优先级 | 模块 | 功能点 | 预计工时 |
|--------|------|--------|---------|
| P1 | 权限菜单配置 | 大屏菜单权限配置 | 1-2天 |
| P2 | 简单 CRUD | 基础数据管理功能 | 2-3天 |
| P3 | AI 知识库 | 文档写入流程 | 3-5天 |

### 4.2 方向一：权限菜单配置

**目标：** 将加密货币和美股大屏纳入系统权限管理

**开发内容:**
1. **菜单配置 SQL**
   - 插入 `sys_permission` 菜单表
   - 配置菜单父子关系
   - 设置菜单图标和排序

2. **权限控制**
   ```java
   // Controller 添加权限注解
   @RequiresPermissions("report:crypto:view")
   @GetMapping("/list")
   public Result<List<CryptoVO>> list() { ... }
   ```

3. **角色分配**
   - 在系统管理-角色管理中配置权限
   - 为管理员角色分配大屏查看权限

**文件位置:**
- 菜单 SQL：`db/insert_menu_report.sql`
- 权限配置：`sys_permission` 表

**预期效果:**
- 大屏菜单显示在系统菜单栏
- 支持按钮级权限控制
- 支持数据权限隔离

---

### 4.3 方向二：简单 CRUD

**目标：** 实现基础数据管理功能

**开发内容:**
1. **加密货币 CRUD**
   - 列表查询（分页、排序、筛选）
   - 新增/编辑/删除币种
   - Excel 导入导出

2. **美股数据 CRUD**
   - 列表查询（按行业筛选）
   - 股票信息管理
   - 数据批量更新

3. **使用 Online 低代码生成**
   ```
   路径：系统管理 -> Online表单开发 -> 代码生成
   步骤：
   1. 导入表 crypto_weekly / us_stock_weekly
   2. 配置表单字段和校验规则
   3. 生成前后端代码
   4. 配置菜单权限
   ```

**文件位置:**
- Online 配置：`/views/online/cgform/`
- 生成代码：`src/main/java/.../modules/report/`

**预期效果:**
- 管理后台可维护基础数据
- 支持数据导入导出
- 完整的增删改查功能

---

### 4.4 方向三：AI 知识库文档写入流程

**目标：** 集成 AI 知识库，支持大屏文档智能问答

**开发内容:**
1. **知识库文档上传**
   - 支持 PDF、Word、Markdown 上传
   - 文档解析和向量化存储
   - 文档分类管理

2. **RAG 检索增强**
   ```
   流程：
   用户提问 -> 向量检索 -> 大模型生成回答 -> 返回答案和引用
   ```

3. **大屏智能问答组件**
   - 在加密货币/美股大屏添加 AI 助手入口
   - 支持自然语言查询（如"比特币最新价格走势如何"）
   - 显示相关图表和数据

4. **数据同步流程**
   - 定时将大屏数据写入知识库
   - 支持历史数据对比分析
   - 生成数据周报/月报

**文件位置:**
- AI 模块：`jeecg-boot-module-airag/`
- 知识库服务：`AiragKnowledgeService.java`
- 前端组件：`/views/airag/knowledge/`

**预期效果:**
- 用户可通过自然语言查询数据
- AI 自动生成分析报告
- 支持多轮对话和上下文理解

---

### 4.5 其他可选方向

| 方向 | 说明 | 优先级 |
|------|------|--------|
| 数据推送 | WebSocket 实时推送行情数据 | P4 |
| 移动端适配 | 大屏 H5 版本 | P4 |
| 数据预警 | 价格异常提醒 | P5 |
| 多语言 | 英文版大屏 | P5 |
| 性能优化 | Redis 缓存、分页优化 | P4 |

---

## 五、开发规范

### 5.1 代码规范

**后端:**
```java
// 修改标记格式
//update-begin---author:claude---date:2026-05-10 for:[需求]新增加密货币大屏功能-----------
// 代码内容
//update-end---author:claude---date:2026-05-10 for:[需求]新增加密货币大屏功能-----------

// Controller 返回统一封装
return Result.OK(data);
return Result.OK("操作成功", data);
return Result.error("操作失败: " + e.getMessage());
```

**前端:**
```typescript
// API 调用使用 defHttp
import { defHttp } from '/@/utils/http/axios'

const getCryptoList = () => {
  return defHttp.get({ url: '/test/crypto/list' })
}

// 组件命名使用 PascalCase
// 文件命名使用 kebab-case
```

### 5.2 提交规范

```
类型: 简要描述

详细描述（可选）

Co-Authored-By: Claude <noreply@anthropic.com>
```

**类型:**
- `feat`: 新功能
- `fix`: 修复
- `docs`: 文档
- `style`: 格式（不影响代码运行）
- `refactor`: 重构
- `test`: 测试
- `chore`: 构建过程/辅助工具

---

## 六、参考资料

### 6.1 官方文档
- [JeecgBoot 文档](https://help.jeecg.com)
- [JeecgBoot Skills](https://jeecg.com/skills)
- [CoinGecko API](https://www.coingecko.com/api)

### 6.2 项目文件
- [README.md](README.md) - 项目介绍和快速开始
- [DEVELOPMENT_GUIDE.md](DEVELOPMENT_GUIDE.md) - 二次开发入口清单
- [PROJECT_NOTES.md](PROJECT_NOTES.md) - 本项目笔记

### 6.3 常用命令

```bash
# 后端
mvn spring-boot:run                    # 启动服务
mvn clean package -DskipTests          # 打包
mvn clean install                      # 安装依赖

# 前端
pnpm dev                               # 启动开发
pnpm build                             # 生产构建
pnpm install                           # 安装依赖

# 数据库
mvn flyway:repair                      # 修复 Flyway
mvn flyway:migrate                     # 执行迁移
```

---

**文档版本:** v1.0  
**创建日期:** 2026-05-10  
**更新记录:**
- 2026-05-10: 初版，汇总启动依赖、接口、报错处理、开发规划
