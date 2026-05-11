# NovaBoot v5.9 本地 RAG 知识库搭建指南

> 基于项目已有的 `airag` 模块，使用 **Ollama 本地模型** + **PostgreSQL pgvector** 搭建完整的本地知识库。

---

## 架构概览

```
┌─────────────┐     ┌─────────────────┐     ┌──────────────────────┐
│  用户提问    │────▶│  airag 知识库    │────▶│  PostgreSQL pgvector │
│  (浏览器)    │     │  (JeecgBoot)     │     │  (向量存储)          │
└─────────────┘     └────────┬────────┘     └──────────────────────┘
                             │
                      ┌──────▼──────┐
                      │   Ollama    │
                      │ (本地模型)   │
                      │ Qwen2.5:7B  │
                      └─────────────┘
```

---

## 第一步：安装 Ollama（本地大模型运行器）

### 1. 下载安装

访问 [https://ollama.com/download](https://ollama.com/download) 下载 Windows 版本。

或直接 PowerShell 安装：

```powershell
# 下载并安装 Ollama
irm https://ollama.com/install.sh | sh  # Linux
# Windows 下载: https://ollama.com/download/OllamaSetup.exe
```

### 2. 下载模型

```bash
# 对话模型（推荐 7B，显存 8GB 以上）
ollama pull qwen2.5:7b

# 向量模型（用于文档向量化）
ollama pull nomic-embed-text

# 查看已下载模型
ollama list
```

### 3. 验证

```bash
# 测试对话
ollama run qwen2.5:7b "你好，请用一句话介绍自己"

# 测试向量模型
curl http://localhost:11434/api/embeddings -d '{
  "model": "nomic-embed-text",
  "prompt": "你好世界"
}'
```

**默认 API 地址：** `http://localhost:11434`

---

## 第二步：安装 PostgreSQL + pgvector（向量数据库）

### 方案 A：Docker（推荐，一条命令搞定）

```bash
# 确保 Docker Desktop 已启动
docker run -d \
  --name jeecg-pgvector \
  -p 5432:5432 \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=vector_db \
  ankane/pgvector:latest
```

### 方案 B：项目 docker-compose

```bash
cd D:\NovaBoot-v5.9
docker-compose up -d jeecg-boot-pgvector
```

### 方案 C：手动安装（无 Docker 环境）

1. 下载 PostgreSQL：https://www.postgresql.org/download/windows/
2. 安装时勾选 pgAdmin
3. 打开 pgAdmin，执行以下 SQL 启用向量扩展：

```sql
-- 创建数据库
CREATE DATABASE vector_db;

-- 连接后启用扩展
\c vector_db
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS hstore;

-- 验证
SELECT * FROM pg_extension WHERE extname = 'vector';
```

### 验证连接

```bash
# 使用 psql 测试
psql -h localhost -p 5432 -U postgres -d vector_db -c "SELECT version();"
```

---

## 第三步：配置 JeecgBoot 连接本地模型

编辑 `jeecg-boot/jeecg-module-system/jeecg-system-start/src/main/resources/application-dev.yml`：

```yaml
jeecg:
  ai-rag:
    # AI流程敏感节点
    allow-sensitive-nodes: sql,stdio
    embed-store:
      host: 127.0.0.1
      port: 5432
      database: vector_db          # 改为 vector_db
      user: postgres
      password: postgres
      table: embeddings
```

### 在系统中配置 Ollama 模型

启动系统后，进入 **AI应用 → AI模型管理**，添加以下模型：

| 配置项 | 值 |
|--------|-----|
| 提供商 | `Ollama` |
| 类型 | `LLM` |
| 模型名称 | `qwen2.5:7b` |
| Base URL | `http://localhost:11434` |
| API Key | （留空） |
| 温度 | 0.7 |
| 最大 Token | 4096 |

再添加一个向量模型：

| 配置项 | 值 |
|--------|-----|
| 提供商 | `Ollama` |
| 类型 | `EMBED` |
| 模型名称 | `nomic-embed-text` |
| Base URL | `http://localhost:11434` |
| API Key | （留空） |

---

## 第四步：创建知识库并上传文档

### 4.1 创建知识库

1. 进入 **AI应用 → 知识库管理**
2. 点击 **新建知识库**
3. 填写：
   - 名称：`我的本地知识库`
   - 描述：`个人知识文档集合`
   - 向量模型：选择刚才配置的 `nomic-embed-text`
   - 分块大小：`1000`（字符）
   - 相似度阈值：`0.75`
   - Top-N 结果数：`5`

### 4.2 上传文档

支持的文档格式：
- **文本文件**：`.txt`, `.md`
- **办公文档**：`.pdf`, `.docx`, `.pptx`, `.xlsx`
- **网页链接**：直接输入 URL 自动抓取

**上传方式：**
1. **文件上传**：直接拖拽文件到知识库页面
2. **ZIP 批量导入**：打包多个文档一次性导入
3. **网页抓取**：输入网页 URL 自动转换为 Markdown

### 4.3 文档处理流程

```
上传文档 → Apache Tika 解析 → 文本分块 → Ollama 向量化 → 存入 pgvector
```

### 4.4 查看向量数据

```sql
-- 查看向量表结构
\c vector_db
\d embeddings

-- 查看已入库文档数量
SELECT COUNT(*) FROM embeddings;

-- 查看知识库文档
SELECT * FROM airag_knowledge_doc ORDER BY create_time DESC;
```

---

## 第五步：创建 AI 应用（基于知识库对话）

### 5.1 创建应用

1. 进入 **AI应用 → AI应用管理**
2. 点击 **新建应用**
3. 填写：
   - 名称：`本地知识库助手`
   - 描述：`基于本地知识库的 AI 助手`
   - 选择 LLM 模型：`qwen2.5:7b`
   - 关联知识库：选择刚创建的知识库
   - 提示词模板：
     ```
     你是一个专业的 AI 助手。请基于以下参考信息回答用户问题：

     {knowledge}

     用户问题：{question}

     如果参考信息不足以回答问题，请坦诚告知，不要编造答案。
     ```

### 5.2 开始对话

1. 进入 **AI应用 → AI聊天**
2. 选择刚创建的应用
3. 输入问题，系统会：
   - 自动检索知识库相关文档
   - 将检索结果作为上下文发送给 LLM
   - 返回带有引用的回答

---

## 硬件要求参考

| 配置 | 最低要求 | 推荐配置 |
|------|---------|---------|
| CPU | 4 核 | 8 核+ |
| 内存 | 8GB | 16GB+ |
| 显存（GPU） | 4GB | 8GB+ |
| 硬盘 | 20GB 可用空间 | SSD 50GB+ |

**无 GPU 也可以运行**（使用 CPU 推理），但速度会慢一些。

---

## 常用模型推荐

### LLM 对话模型（按显存大小选择）

| 模型 | 显存需求 | 特点 |
|------|---------|------|
| `llama3.2:3b` | 4GB | 轻量级，响应快 |
| `qwen2.5:7b` | 8GB | 中文能力强，推荐 |
| `qwen2.5:14b` | 16GB | 更高质量 |
| `mistral:7b` | 8GB | 英文能力强 |

### 向量模型

| 模型 | 维度 | 特点 |
|------|------|------|
| `nomic-embed-text` | 768 | 轻量，推荐 |
| `mxbai-embed-large` | 1024 | 高质量 |
| `all-minilm` | 384 | 最轻量 |

---

## 常见问题

### Q1：Ollama 启动失败？

```bash
# 检查服务状态
ollama serve

# 查看日志
tail -f ~/.ollama/logs/server.log
```

### Q2：向量检索结果为空？

1. 确认文档已成功上传并向量化
2. 检查知识库是否关联到正确的向量模型
3. 尝试降低相似度阈值（0.75 → 0.6）

### Q3：对话响应很慢？

1. 使用更小的模型（如 3B 代替 7B）
2. 减少 Top-N 结果数（5 → 3）
3. 减少分块大小（1000 → 500）

### Q4：PostgreSQL 连接失败？

```bash
# 检查端口是否开放
netstat -an | findstr 5432

# 测试连接
psql -h localhost -p 5432 -U postgres -c "SELECT 1;"
```

---

## 进阶：自建 AI 搜索替代

项目默认配置了 SearXNG 作为联网搜索。如果想完全本地化，可以部署 SearXNG：

```bash
docker run -d --name searxng \
  -p 8080:8080 \
  -e BASE_URL=http://localhost:8080 \
  searxng/searxng:latest
```

然后在 `application-dev.yml` 中配置：

```yaml
jeecg:
  ai-rag:
    searxng:
      url: http://localhost:8080
```

---

## 快速检查清单

完成以下所有步骤即表示本地 RAG 知识库已搭建成功：

- [ ] Ollama 安装并启动（`http://localhost:11434` 可访问）
- [ ] 下载对话模型 `qwen2.5:7b`
- [ ] 下载向量模型 `nomic-embed-text`
- [ ] PostgreSQL + pgvector 运行中（端口 5432 开放）
- [ ] `application-dev.yml` 中配置了正确的向量库地址
- [ ] 在系统管理中添加了 Ollama LLM 模型
- [ ] 在系统管理中添加了 Ollama 向量模型
- [ ] 创建了知识库并上传了文档
- [ ] 创建了 AI 应用并关联知识库
- [ ] 通过聊天界面成功获得基于知识库的回答

---

**文档版本：** v1.0  
**创建日期：** 2026-05-10
