# Regent 配置说明

## 概述

[Regent](https://github.com/regent-vcs/regent) 是 AI Agent 的版本控制系统，用于追踪 Claude Code 的活动记录。

## 安装位置

- 可执行文件: `re_gent\rgt.exe`
- 数据存储: `.regent/` (类似 `.git/`)

## 常用命令

```bash
# 查看 agent 活动历史
.\re_gent\rgt.exe log

# 查看当前状态
.\re_gent\rgt.exe status

# 列出所有 session
.\re_gent\rgt.exe sessions

# 查看某个 step 的详细信息
.\re_gent\rgt.exe show <step-hash>

# 查看某行代码是由哪个 prompt 创建的
.\re_gent\rgt.exe blame <file>:<line>

# 查看版本信息
.\re_gent\rgt.exe version
```

## Hooks 配置

已在 `.claude\settings.json` 中配置了自动追踪 hooks：
- 每次 `Write`、`Edit`、`Bash` 工具调用后自动记录
- 通过 `rgt hook` 命令捕获工具调用详情

## 文件过滤

- `.gitignore` - 已添加 `.regent/` 排除，不会被提交到 git
- `.regentignore` - Regent 自身的文件过滤规则

## 数据结构

```
.regent/
├── objects/     # 内容寻址存储 (BLAKE3 哈希)
├── refs/        # Session 指针
├── index.db     # SQLite 查询索引
└── config.toml  # 配置文件
```

## 与 Git 的区别

| 特性 | Git | Regent |
|------|-----|--------|
| 追踪代码 | ✅ | ✅ |
| 追踪 Agent 活动 | ❌ | ✅ |
| 查看哪行代码由哪个 prompt 创建 | ❌ | ✅ |
| 对话历史 | ❌ | ✅ |
| 并发 Session | ⚠️ 冲突 | ✅ 独立分支 |

Regent 是对 Git 的补充，不是替代。两者配合使用。
