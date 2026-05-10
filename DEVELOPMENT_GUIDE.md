# NovaBoot v5.9 二次开发入口清单

> 本文档汇总 NovaBoot v5.9 项目中所有新增功能的开发入口，包括 Controller、Service、Mapper、前端页面、路由配置和菜单 SQL。

---

## 目录

1. [新增功能概览](#新增功能概览)
2. [后端开发入口](#后端开发入口)
3. [前端开发入口](#前端开发入口)
4. [数据库与 SQL](#数据库与-sql)
5. [配置与路由](#配置与路由)

---

## 新增功能概览

| 功能模块 | 说明 | 状态 |
|---------|------|------|
| 加密货币实时大屏 | 基于 CoinGecko API，展示 Top 50 加密货币实时行情 | ✅ 已完成 |
| 美股实时数据大屏 | 80 只热门美股实时行情展示，支持 7 大行业分类 | ✅ 已完成 |
| 首页入口卡片 | 快速访问两大屏的快捷入口 | ✅ 已完成 |
| Quartz 定时任务 | 每 5 分钟自动同步数据到数据库 | ✅ 已完成 |

---

## 后端开发入口

### 1. Controller 层

#### 1.1 加密货币报表 Controller
```
路径: jeecg-boot/jeecg-module-system/jeecg-system-start/src/main/java/org/jeecg/modules/system/controller/CryptoReportController.java
```

**接口列表:**
| 接口地址 | 方法 | 说明 |
|---------|------|------|
| `/test/crypto/list` | GET | 获取加密货币列表（数据库） |
| `/test/crypto/realtime` | GET | 获取实时加密货币数据（直接 API） |
| `/test/crypto/sync` | POST | 手动触发数据同步 |

#### 1.2 美股报表 Controller
```
路径: jeecg-boot/jeecg-module-system/jeecg-system-start/src/main/java/org/jeecg/modules/system/controller/UsStockReportController.java
```

**接口列表:**
| 接口地址 | 方法 | 说明 |
|---------|------|------|
| `/test/usstock/list` | GET | 获取美股列表（数据库） |
| `/test/usstock/realtime` | GET | 获取实时美股数据（直接生成） |
| `/test/usstock/sync` | POST | 手动触发数据同步 |

---

### 2. Service 层

#### 2.1 CoinGecko 服务
```
路径: jeecg-boot/jeecg-module-system/jeecg-system-start/src/main/java/org/jeecg/modules/system/service/CoinGeckoService.java
```

**主要方法:**
```java
// 获取实时加密货币数据
List<Map<String, Object>> fetchCryptoData()

// 同步数据到数据库
void syncToDatabase()
```

#### 2.2 美股市场服务
```
路径: jeecg-boot/jeecg-module-system/jeecg-system-start/src/main/java/org/jeecg/modules/system/service/UsStockMarketService.java
```

**主要方法:**
```java
// 获取实时美股数据（80只股票模拟数据）
List<Map<String, Object>> fetchRealTimeData()

// 同步数据到数据库
void syncToDatabase()
```

---

### 3. Job 层（Quartz 定时任务）

#### 3.1 加密货币同步任务
```
路径: jeecg-boot/jeecg-module-system/jeecg-system-start/src/main/java/org/jeecg/modules/system/job/CryptoDataSyncJob.java
```

**Cron 表达式:** `0 */5 * * * ?`（每 5 分钟执行）

#### 3.2 美股同步任务
```
路径: jeecg-boot/jeecg-module-system/jeecg-system-start/src/main/java/org/jeecg/modules/system/job/UsStockDataSyncJob.java
```

**Cron 表达式:** `0 */5 * * * ?`（每 5 分钟执行）

#### 3.3 定时任务配置
```
路径: jeecg-boot/jeecg-module-system/jeecg-system-start/src/main/java/org/jeecg/modules/system/config/QuartzJobConfig.java
```

---

### 4. VO（值对象）

#### 4.1 加密货币 VO
```
路径: 位于 CryptoReportController.java 内部类
类名: CryptoReportController.CryptoVO
```

**字段:**
| 字段名 | 类型 | 说明 |
|-------|------|------|
| symbol | String | 币种代码 |
| name | String | 币种名称 |
| price | BigDecimal | 当前价格 |
| changePct24h | BigDecimal | 24小时涨跌幅% |
| changePct7d | BigDecimal | 7天涨跌幅% |
| volume24h | Long | 24小时成交量 |
| marketCap | BigDecimal | 市值 |
| marketCapRank | Integer | 市值排名 |

#### 4.2 美股 VO
```
路径: 位于 UsStockReportController.java 内部类
类名: UsStockReportController.UsStockVO
```

**字段:**
| 字段名 | 类型 | 说明 |
|-------|------|------|
| symbol | String | 股票代码 |
| name | String | 股票名称 |
| sector | String | 行业分类 |
| closePrice | BigDecimal | 收盘价 |
| changePct | BigDecimal | 涨跌幅% |
| volume | Long | 成交量 |
| marketCap | String | 市值 |

---

### 5. Mapper（使用 JdbcTemplate）

项目使用 Spring JdbcTemplate 进行数据库操作，无需定义 Mapper 接口。

```java
// 在 Service 中注入使用
private final JdbcTemplate jdbcTemplate;

// 查询示例
String sql = "SELECT * FROM us_stock_weekly ORDER BY change_pct DESC";
List<UsStockVO> list = jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(UsStockVO.class));
```

---

## 前端开发入口

### 1. Vue 页面

#### 1.1 加密货币大屏页面
```
路径: jeecgboot-vue3/src/views/report/crypto/bigscreen/index.vue
路由: /report/crypto
```

**主要功能:**
- Top 50 加密货币行情展示
- 7 日价格趋势折线图
- 市值分布饼图
- 实时数据自动刷新（每 5 分钟）
- 平滑数据过渡动画

#### 1.2 美股大屏页面
```
路径: jeecgboot-vue3/src/views/report/usstock/bigscreen/index.vue
路由: /report/usstock/dashboard
```

**主要功能:**
- 80 只热门美股行情展示
- 7 大行业分类展示
- Top10 涨跌幅榜单
- 行业分布饼图
- 平滑数据过渡动画（800ms easeOutCubic）
- 本地价格模拟（3 秒间隔）

#### 1.3 美股管理页面
```
路径: jeecgboot-vue3/src/views/report/usstock/index.vue
路由: /report/usstock
```

**主要功能:**
- 美股数据列表展示
- 数据筛选和排序

#### 1.4 首页入口卡片
```
路径: jeecgboot-vue3/src/views/dashboard/Analysis/homePage/IndexChart.vue
```

**主要修改:**
- 添加了两个入口卡片（加密货币、美股）
- 点击卡片跳转到大屏页面

---

### 2. 路由配置

#### 2.1 美股路由配置
```
路径: jeecgboot-vue3/src/router/routes/modules/report/usstock.ts
```

```typescript
const usstock: AppRouteModule = {
  path: '/report',
  name: 'Report',
  component: LAYOUT,
  redirect: '/report/usstock',
  meta: {
    orderNo: 100,
    icon: 'ion:analytics-outline',
    title: '报表中心',
  },
  children: [
    {
      path: 'usstock',
      name: 'UsStockReport',
      component: () => import('/@/views/report/usstock/index.vue'),
      meta: {
        title: '美股涨跌行情',
        ignoreKeepAlive: true,
      },
    },
  ],
};
```

#### 2.2 静态路由配置
```
路径: jeecgboot-vue3/src/router/routes/staticRouter.ts
```

**新增路由:**
- `/report/crypto` → 加密货币大屏
- `/report/usstock/dashboard` → 美股大屏

---

### 3. API 调用

前端通过 Axios 调用后端接口：

```typescript
// 加密货币 API
defHttp.get('/test/crypto/list')           // 获取列表
defHttp.get('/test/crypto/realtime')       // 获取实时数据
defHttp.post('/test/crypto/sync', {})      // 手动同步

// 美股 API
defHttp.get('/test/usstock/list')          // 获取列表
defHttp.get('/test/usstock/realtime')      // 获取实时数据
defHttp.post('/test/usstock/sync', {})     // 手动同步
```

---

## 数据库与 SQL

### 1. Flyway 迁移脚本

#### 1.1 美股数据表
```
路径: jeecg-boot/jeecg-module-system/jeecg-system-start/src/main/resources/flyway/sql/mysql/V3.9.3_1__usstock_more.sql
```

**表结构:**
```sql
CREATE TABLE `us_stock_weekly` (
  `id` int NOT NULL AUTO_INCREMENT,
  `symbol` varchar(20) NOT NULL COMMENT '股票代码',
  `name` varchar(100) DEFAULT NULL COMMENT '股票名称',
  `sector` varchar(50) DEFAULT NULL COMMENT '行业',
  `close_price` decimal(18,4) DEFAULT NULL COMMENT '收盘价',
  `prev_close` decimal(18,4) DEFAULT NULL COMMENT '前收盘价',
  `change` decimal(18,4) DEFAULT NULL COMMENT '涨跌额',
  `change_pct` decimal(18,4) DEFAULT NULL COMMENT '涨跌幅%',
  `open_price` decimal(18,4) DEFAULT NULL COMMENT '开盘价',
  `high_price` decimal(18,4) DEFAULT NULL COMMENT '最高价',
  `low_price` decimal(18,4) DEFAULT NULL COMMENT '最低价',
  `volume` bigint DEFAULT NULL COMMENT '成交量',
  `market_cap` varchar(20) DEFAULT NULL COMMENT '市值',
  `trade_date` varchar(20) DEFAULT NULL COMMENT '交易日期',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_symbol_date` (`symbol`,`trade_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='美股周报数据';
```

**数据量:** 80 条初始数据（7 大行业）

#### 1.2 加密货币数据表
```
路径: jeecg-boot/jeecg-module-system/jeecg-system-start/src/main/resources/flyway/sql/mysql/V3.9.3_2__crypto_data.sql
```

**表结构:**
```sql
CREATE TABLE `crypto_weekly` (
  `id` int NOT NULL AUTO_INCREMENT,
  `symbol` varchar(20) NOT NULL COMMENT '币种代码',
  `name` varchar(100) DEFAULT NULL COMMENT '币种名称',
  `price` decimal(18,8) DEFAULT NULL COMMENT '当前价格(USD)',
  `price_btc` decimal(18,8) DEFAULT NULL COMMENT 'BTC计价',
  `change_24h` decimal(18,8) DEFAULT NULL COMMENT '24小时涨跌额',
  `change_pct_24h` decimal(18,4) DEFAULT NULL COMMENT '24小时涨跌幅%',
  `change_7d` decimal(18,4) DEFAULT NULL COMMENT '7天涨跌幅%',
  `high_24h` decimal(18,8) DEFAULT NULL COMMENT '24小时最高价',
  `low_24h` decimal(18,8) DEFAULT NULL COMMENT '24小时最低价',
  `volume_24h` decimal(20,2) DEFAULT NULL COMMENT '24小时成交量',
  `market_cap` decimal(20,2) DEFAULT NULL COMMENT '市值',
  `market_cap_rank` int DEFAULT NULL COMMENT '市值排名',
  `circulating_supply` decimal(20,2) DEFAULT NULL COMMENT '流通量',
  `total_supply` decimal(20,2) DEFAULT NULL COMMENT '总供应量',
  `max_supply` decimal(20,2) DEFAULT NULL COMMENT '最大供应量',
  `trade_date` varchar(20) DEFAULT NULL COMMENT '数据日期',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_symbol_date` (`symbol`,`trade_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='加密货币周报数据';
```

**数据量:** 55 条初始数据（Layer1、DeFi、Meme、Layer2、AI 等分类）

---

### 2. 菜单 SQL

菜单通过代码生成器模板自动生成，模板路径：
```
jeecg-boot/config/jeecg/code-template-online/default/one/java/${bussiPackage}/${entityPackage}/vue3/V${currentDate}_1__menu_insert_${entityName}.sql
```

---

## 配置与路由

### 1. 后端配置

#### 1.1 应用配置
```
路径: jeecg-boot/jeecg-module-system/jeecg-system-start/src/main/resources/application-dev.yml
```

**关键配置项:**
```yaml
server:
  port: 8080
  servlet:
    context-path: /jeecg-boot

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/nova_boot?characterEncoding=UTF-8&useUnicode=true&useSSL=false
    username: root
    password: root

  redis:
    host: 127.0.0.1
    port: 6379
```

#### 1.2 Quartz 配置
```
路径: jeecg-boot/jeecg-module-system/jeecg-system-start/src/main/java/org/jeecg/modules/system/config/QuartzJobConfig.java
```

---

### 2. 前端配置

#### 2.1 环境配置
```
路径: jeecgboot-vue3/.env.development
```

**关键配置项:**
```
VITE_GLOB_API_URL=/jeecg-boot
VITE_PORT=3100
```

#### 2.2 代理配置
```
路径: jeecgboot-vue3/vite.config.ts
```

**代理设置:**
```typescript
proxy: {
  '/jeecg-boot': {
    target: 'http://localhost:8080',
    changeOrigin: true,
    ws: false,
    rewrite: (path) => path.replace(/^\/jeecg-boot/, '')
  }
}
```

---

## 开发顺序建议

### 场景 1: 修改大屏展示效果
1. 前端页面：`jeecgboot-vue3/src/views/report/*/bigscreen/index.vue`
2. 图表组件：`jeecgboot-vue3/src/components/chart/`

### 场景 2: 修改数据源逻辑
1. Service 层：`jeecg-boot/.../service/*Service.java`
2. Controller 层：`jeecg-boot/.../controller/*Controller.java`
3. 前端 API 调用：检查对应 Vue 文件中的接口调用

### 场景 3: 添加新的定时任务
1. 创建 Job 类：`jeecg-boot/.../job/xxxJob.java`
2. 配置触发器：`jeecg-boot/.../config/QuartzJobConfig.java`

### 场景 4: 修改数据库表结构
1. 创建 Flyway 脚本：`jeecg-boot/.../flyway/sql/mysql/Vx.x.x__xxx.sql`
2. 重启应用自动执行

### 场景 5: 添加新的大屏入口
1. 首页卡片：`jeecgboot-vue3/src/views/dashboard/Analysis/homePage/IndexChart.vue`
2. 路由配置：`jeecgboot-vue3/src/router/routes/staticRouter.ts`
3. 新增页面：`jeecgboot-vue3/src/views/report/xxx/bigscreen/index.vue`

---

## 注意事项

1. **代码修改标记**: 所有修改需添加 `update-begin` / `update-end` 注释
2. **权限控制**: 大屏接口已配置在 Shiro 白名单中，无需登录即可访问 `/test/*`
3. **数据刷新**: 实时数据每 5 分钟从 API 同步，本地模拟每 3 秒更新
4. **浏览器兼容**: 支持 Chrome、Firefox、Edge 最新版本

---

## 附录：行业分类

### 美股行业分类（7 大行业）
| 行业代码 | 行业名称 | 股票数量 |
|---------|---------|---------|
| Technology | 科技 | 15 |
| Finance | 金融 | 12 |
| Healthcare | 医疗健康 | 10 |
| Consumer | 消费 | 10 |
| Industrial | 工业 | 10 |
| Energy | 能源 | 8 |
| Telecom | 通信 | 5 |

### 加密货币分类
| 分类 | 说明 | 代表币种 |
|------|------|---------|
| Layer1 | 主流公链 | BTC, ETH, SOL, BNB |
| DeFi | 去中心化金融 | LINK, UNI, AAVE, MKR |
| Meme | 模因币 | DOGE, SHIB, PEPE |
| Layer2 | 扩容方案 | MATIC, ARB, OP |
| AI | 人工智能 | FET, RNDR, TAO |

---

**文档版本:** v1.0  
**最后更新:** 2026-05-10  
**作者:** Claude
