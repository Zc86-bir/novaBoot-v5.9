# NovaBoot v5.9

基于 [JeecgBoot 3.9.2](https://github.com/jeecgboot/JeecgBoot) 的企业级 AI 低代码开发平台，集成实时金融数据大屏功能。

[![License](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg)](https://github.com/jeecgboot/JeecgBoot/blob/master/LICENSE)
[![Version](https://img.shields.io/badge/version-5.9-brightgreen.svg)]()
[![JeecgBoot](https://img.shields.io/badge/JeecgBoot-3.9.2-orange.svg)](https://github.com/jeecgboot/JeecgBoot)

---

## 项目简介

NovaBoot 是一款基于 JeecgBoot 3.9.2 构建的企业级 AI 低代码开发平台，在保留 JeecgBoot 全部功能的基础上，新增**实时金融数据可视化大屏**功能模块。

### 核心特性

- **AI 低代码开发**：支持 "低代码 + 零代码" 双模式，AI 自动生成前后端代码
- **实时加密货币大屏**：基于 CoinGecko API，展示 Top 50 加密货币实时行情
- **实时美股数据大屏**：80 只热门美股实时行情展示，支持 7 大行业分类
- **定时数据同步**：Quartz 定时任务，每 5 分钟自动同步数据到数据库
- **平滑动画效果**：数据过渡动画、实时价格模拟，提供流畅的视觉体验

---

## 新增功能

### 加密货币实时大屏
- 对接 CoinGecko API 获取实时数据
- Top 50 加密货币行情展示
- 实时价格、涨跌幅、市值等关键指标
- 7 日价格趋势折线图
- 首页快捷入口卡片

### 美股数据实时大屏
- 80 只热门美股实时数据展示
- 7 大行业分类：科技、金融、医疗、消费、工业、能源、电信
- 实时价格、涨跌幅、成交量等数据
- 行业分布饼图、Top10 涨跌幅榜单
- 平滑数据过渡动画（800ms easeOutCubic）
- 本地价格模拟（3 秒间隔）

### 技术实现
- **后端**：Spring Boot + Quartz 定时任务 + MyBatis-Plus
- **前端**：Vue 3 + TypeScript + Vite + ECharts
- **数据源**：CoinGecko API（加密货币）/ 模拟实时数据（美股）
- **数据同步**：每 5 分钟自动同步到数据库

---

## 技术栈

### 后端
- **基础框架**：Spring Boot 3.5.5
- **微服务**：Spring Cloud Alibaba 2023.0.3.3
- **持久层**：MyBatis-Plus 3.5.12
- **定时任务**：Quartz Scheduler
- **数据库**：MySQL 5.7+（支持达梦、人大金仓等国产数据库）
- **缓存**：Redis
- **安全**：Apache Shiro 2.0.4 + JWT

### 前端
- **框架**：Vue 3.0 + TypeScript
- **构建工具**：Vite 6
- **UI 组件**：Ant Design Vue 4
- **图表库**：ECharts
- **状态管理**：Pinia

---

## 快速开始

### 环境要求
- **JDK**：17/21/24/25
- **Node.js**：20.19+ 或 22.12+
- **pnpm**：9+
- **MySQL**：5.7+
- **Redis**：5.0+

### 1. 克隆项目

```bash
git clone https://github.com/Zc86-bir/novaBoot-v5.9.git
cd novaBoot-v5.9
```

### 2. 初始化数据库

```bash
# 创建数据库
create database nova_boot default charset utf8mb4 collate utf8mb4_unicode_ci;

# 执行初始化 SQL（位于 jeecg-boot/db 目录）
```

### 3. 启动后端

```bash
cd jeecg-boot/jeecg-module-system/jeecg-system-start

# 修改 application-dev.yml 中的数据库和 Redis 配置
# 启动应用
mvn spring-boot:run
```

后端服务默认端口：`8080`

### 4. 启动前端

```bash
cd jeecgboot-vue3

pnpm install
pnpm dev
```

前端服务默认端口：`3100`

### 5. 访问系统

- 后台地址：http://localhost:3100
- 默认账号：`admin`
- 默认密码：`123456`

---

## 项目结构

```
novaBoot-v5.9/
├── jeecg-boot/                          # 后端 Java 项目
│   ├── jeecg-module-system/
│   │   └── jeecg-system-start/
│   │       ├── src/main/java/
│   │       │   └── org/jeecg/modules/
│   │       │       ├── system/
│   │       │       │   ├── config/
│   │       │       │   │   └── QuartzJobConfig.java       # Quartz 定时任务配置
│   │       │       │   ├── controller/
│   │       │       │   │   ├── CryptoReportController.java   # 加密货币 API
│   │       │       │   │   └── UsStockReportController.java  # 美股 API
│   │       │       │   ├── job/
│   │       │       │   │   ├── CryptoDataSyncJob.java        # 加密货币同步任务
│   │       │       │   │   └── UsStockDataSyncJob.java       # 美股同步任务
│   │       │       │   └── service/
│   │       │       │       ├── CoinGeckoService.java         # CoinGecko API 服务
│   │       │       │       └── UsStockMarketService.java     # 美股数据服务
│   │       │       └── report/                          # 报表模块
│   │       └── src/main/resources/
│   │           └── flyway/sql/mysql/
│   │               ├── V3.9.3_1__usstock_more.sql      # 美股数据表
│   │               └── V3.9.3_2__crypto_data.sql       # 加密货币数据表
│   └── ...
│
├── jeecgboot-vue3/                      # 前端 Vue3 项目
│   ├── src/
│   │   ├── views/
│   │   │   ├── dashboard/
│   │   │   │   └── Analysis/
│   │   │   │       └── homePage/
│   │   │   │           └── IndexChart.vue              # 首页入口卡片
│   │   │   └── report/
│   │   │       ├── crypto/
│   │   │       │   └── bigscreen/
│   │   │       │       └── index.vue                   # 加密货币大屏
│   │   │       └── usstock/
│   │   │           └── bigscreen/
│   │   │               └── index.vue                   # 美股数据大屏
│   │   └── ...
│   └── ...
│
└── scripts/                             # 工具脚本
    └── generate_resume_optimized.py     # 简历生成工具
```

---

## API 接口

### 加密货币接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/test/crypto/list` | GET | 获取加密货币列表（数据库） |
| `/test/crypto/realtime` | GET | 获取实时加密货币数据 |
| `/test/crypto/sync` | POST | 手动触发数据同步 |

### 美股接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/test/usstock/list` | GET | 获取美股列表（数据库） |
| `/test/usstock/realtime` | GET | 获取实时美股数据 |
| `/test/usstock/sync` | POST | 手动触发数据同步 |

---

## 定时任务配置

定时任务配置位于 `QuartzJobConfig.java`：

```java
// 加密货币同步：每 5 分钟
@Bean
public Trigger cryptoDataSyncTrigger() {
    return CronScheduleBuilder.cronSchedule("0 */5 * * * ?")
        .withMisfireHandlingInstructionFireAndProceed();
}

// 美股数据同步：每 5 分钟
@Bean
public Trigger usStockDataSyncTrigger() {
    return CronScheduleBuilder.cronSchedule("0 */5 * * * ?")
        .withMisfireHandlingInstructionFireAndProceed();
}
```

---

## 大屏路由

- **加密货币大屏**：`/report/crypto`
- **美股数据大屏**：`/report/usstock/dashboard`

首页已添加入口卡片，点击即可快速访问。

---

## 功能清单

### 基础功能（JeecgBoot 自带）
- ✅ AI 应用平台（AI 助手、知识库、流程编排）
- ✅ 系统管理（用户、角色、菜单、部门、字典）
- ✅ 低代码开发（Online 表单、Online 报表、代码生成器）
- ✅ 数据可视化（JimuReport 报表、JimuBI 大屏）
- ✅ 工作流引擎（Flowable BPMN）
- ✅ 多租户、多数据源、权限控制

### 新增功能（NovaBoot v5.9）
- ✅ 加密货币实时数据大屏
- ✅ 美股实时数据大屏
- ✅ 首页快捷入口卡片
- ✅ 平滑数据过渡动画
- ✅ 定时数据同步任务

---

## 文档与资源

- [JeecgBoot 官方文档](https://help.jeecg.com)
- [JeecgBoot Skills 自然语言编程](https://jeecg.com/skills)
- [CoinGecko API 文档](https://www.coingecko.com/api)

---

## 许可证

本项目基于 [Apache License 2.0](https://github.com/jeecgboot/JeecgBoot/blob/master/LICENSE) 开源协议发布。

---

## 致谢

感谢 [JeecgBoot](https://github.com/jeecgboot/JeecgBoot) 团队提供优秀的企业级低代码开发平台。
