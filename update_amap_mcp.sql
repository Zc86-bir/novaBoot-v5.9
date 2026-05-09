-- 高德 MCP 配置更新脚本
-- 执行方法：在 MySQL 客户端中运行以下 SQL

USE jeecg-boot;

-- 更新高德 MCP 的 API Key
UPDATE airag_mcp
SET endpoint = 'https://mcp.amap.com/sse?key=adaea6afab82893d0ccb4bd0239abcdd',
    update_time = NOW()
WHERE id = '1983474860536475649';

-- 验证更新结果
SELECT id, name, endpoint, status, synced
FROM airag_mcp
WHERE id = '1983474860536475649';
