package org.jeecg.modules.airag.app.handler;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.service.tool.ToolExecutor;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.util.oConvertUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * OpenCLI 工具构建器
 * OpenCLI 将网站和浏览器转换为 AI 可调用的 CLI 接口
 * GitHub: https://github.com/jackwener/opencli
 *
 * 使用前需安装:
 * npm install -g @jackwener/opencli
 * 并安装浏览器扩展
 *
 * @author Claude
 * @date 2026-05-09
 */
@Slf4j
public class OpenCLIToolBuilder {

    /**
     * 构建 OpenCLI 搜索工具
     *
     * @param config OpenCLI 配置
     * @return 工具Map
     */
    public static Map<ToolSpecification, ToolExecutor> buildTools(OpenCLIConfig config) {
        Map<ToolSpecification, ToolExecutor> tools = new HashMap<>();
        if (config == null || !config.isEnabled()) {
            log.warn("[OpenCLI]未启用 OpenCLI，联网搜索不生效");
            return tools;
        }

        // 验证 opencli 是否可用
        if (!isAvailable(config)) {
            log.warn("[OpenCLI]未找到 opencli 命令，请确保已安装: npm install -g @jackwener/opencli");
            return tools;
        }

        // 1. 通用搜索工具
        ToolSpecification searchSpec = ToolSpecification.builder()
                .name("opencli_search")
                .description("通过浏览器搜索互联网上的信息。支持多个平台：hackernews、reddit、zhihu、bilibili、google-scholar等。当你需要获取特定平台的实时信息时使用此工具。")
                .parameters(JsonObjectSchema.builder()
                        .addStringProperty("platform", "搜索平台，可选值：hackernews、reddit、zhihu、bilibili、google-scholar、twitter。默认 hackernews")
                        .addStringProperty("query", "搜索关键词或查询语句")
                        .addNumberProperty("limit", "返回结果数量（可选，默认5条）")
                        .required("query")
                        .build())
                .build();

        ToolExecutor searchExecutor = (toolExecutionRequest, memoryId) -> {
            try {
                JSONObject args = JSONObject.parseObject(toolExecutionRequest.arguments());
                String query = args.getString("query");
                String platform = args.getString("platform");
                int limit = args.getInteger("limit");
                if (limit <= 0 || limit > 20) {
                    limit = config.getDefaultLimit();
                }

                if (oConvertUtils.isEmpty(platform)) {
                    platform = "hackernews";
                }

                // 构建命令
                List<String> command = new ArrayList<>();
                command.add(config.getCommand());
                command.add(platform);
                command.add("search");
                command.add(query);
                command.add("--limit");
                command.add(String.valueOf(limit));
                command.add("-f");
                command.add("json");

                return executeCommand(command, config.getTimeout());

            } catch (Exception e) {
                log.error("[OpenCLI]搜索失败: {}", e.getMessage(), e);
                return "搜索执行失败: " + e.getMessage();
            }
        };

        tools.put(searchSpec, searchExecutor);

        // 2. 热门趋势工具
        ToolSpecification hotSpec = ToolSpecification.builder()
                .name("opencli_hot")
                .description("获取指定平台的热门/ trending 内容。支持平台：hackernews、reddit、zhihu、bilibili、twitter等。")
                .parameters(JsonObjectSchema.builder()
                        .addStringProperty("platform", "平台名称，可选值：hackernews、reddit、zhihu、bilibili、twitter。默认 hackernews")
                        .addNumberProperty("limit", "返回结果数量（可选，默认5条）")
                        .required("platform")
                        .build())
                .build();

        ToolExecutor hotExecutor = (toolExecutionRequest, memoryId) -> {
            try {
                JSONObject args = JSONObject.parseObject(toolExecutionRequest.arguments());
                String platform = args.getString("platform");
                int limit = args.getInteger("limit");
                if (limit <= 0 || limit > 20) {
                    limit = config.getDefaultLimit();
                }

                List<String> command = new ArrayList<>();
                command.add(config.getCommand());
                command.add(platform);
                command.add("hot");
                command.add("--limit");
                command.add(String.valueOf(limit));
                command.add("-f");
                command.add("json");

                return executeCommand(command, config.getTimeout());

            } catch (Exception e) {
                log.error("[OpenCLI]获取热门内容失败: {}", e.getMessage(), e);
                return "获取热门内容失败: " + e.getMessage();
            }
        };

        tools.put(hotSpec, hotExecutor);

        // 3. 网页提取工具
        ToolSpecification extractSpec = ToolSpecification.builder()
                .name("opencli_extract")
                .description("从指定 URL 提取网页内容。使用浏览器自动化获取动态渲染的网页内容，适合 JavaScript 渲染的页面。")
                .parameters(JsonObjectSchema.builder()
                        .addStringProperty("url", "要提取内容的网页URL")
                        .addStringProperty("selector", "CSS选择器，用于定位要提取的内容区域（可选）")
                        .required("url")
                        .build())
                .build();

        ToolExecutor extractExecutor = (toolExecutionRequest, memoryId) -> {
            try {
                JSONObject args = JSONObject.parseObject(toolExecutionRequest.arguments());
                String url = args.getString("url");
                String selector = args.getString("selector");

                List<String> command = new ArrayList<>();
                command.add(config.getCommand());
                command.add("browser");
                command.add("extract");
                command.add(url);
                if (oConvertUtils.isNotEmpty(selector)) {
                    command.add("--selector");
                    command.add(selector);
                }
                command.add("-f");
                command.add("json");

                return executeCommand(command, config.getTimeout());

            } catch (Exception e) {
                log.error("[OpenCLI]网页提取失败: {}", e.getMessage(), e);
                return "网页提取失败: " + e.getMessage();
            }
        };

        tools.put(extractSpec, extractExecutor);

        log.info("[OpenCLI]已配置 {} 个工具，版本: {}", tools.size(), getVersion(config));
        return tools;
    }

    /**
     * 检查 opencli 是否可用
     */
    private static boolean isAvailable(OpenCLIConfig config) {
        try {
            List<String> command = new ArrayList<>();
            command.add(config.getCommand());
            command.add("--version");

            ProcessBuilder pb = new ProcessBuilder(command);
            Process process = pb.start();
            boolean finished = process.waitFor(10, TimeUnit.SECONDS);
            return finished && process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取 opencli 版本
     */
    private static String getVersion(OpenCLIConfig config) {
        try {
            List<String> command = new ArrayList<>();
            command.add(config.getCommand());
            command.add("--version");

            ProcessBuilder pb = new ProcessBuilder(command);
            Process process = pb.start();
            boolean finished = process.waitFor(10, TimeUnit.SECONDS);
            if (finished && process.exitValue() == 0) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    return reader.readLine();
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return "unknown";
    }

    /**
     * 执行命令并返回结果
     */
    private static String executeCommand(List<String> command, int timeoutSeconds) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.environment().putAll(System.getenv());

        // 设置额外的环境变量
        pb.environment().put("OPENCLI_VERBOSE", "1");

        Process process = pb.start();
        boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);

        if (!finished) {
            process.destroyForcibly();
            return "命令执行超时（" + timeoutSeconds + "秒）";
        }

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        StringBuilder errorOutput = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                errorOutput.append(line).append("\n");
            }
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            return "命令执行失败，退出码: " + exitCode + "\n错误信息: " + errorOutput.toString();
        }

        // 解析 JSON 输出
        String result = output.toString().trim();
        if (result.startsWith("[")) {
            // 数组格式，转换为更友好的文本
            try {
                JSONArray results = JSONArray.parseArray(result);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < results.size(); i++) {
                    JSONObject item = results.getJSONObject(i);
                    sb.append(i + 1).append(". ").append(item.getString("title")).append("\n");
                    if (item.containsKey("content") && oConvertUtils.isNotEmpty(item.getString("content"))) {
                        sb.append("   ").append(item.getString("content")).append("\n");
                    }
                    if (item.containsKey("url") && oConvertUtils.isNotEmpty(item.getString("url"))) {
                        sb.append("   链接: ").append(item.getString("url")).append("\n");
                    }
                    if (item.containsKey("author") && oConvertUtils.isNotEmpty(item.getString("author"))) {
                        sb.append("   作者: ").append(item.getString("author")).append("\n");
                    }
                    if (item.containsKey("score") && oConvertUtils.isNotEmpty(item.getString("score"))) {
                        sb.append("   得分: ").append(item.getString("score")).append("\n");
                    }
                    sb.append("\n");
                }
                return "搜索结果（共 " + results.size() + " 条）：\n\n" + sb.toString();
            } catch (Exception e) {
                return result;
            }
        }

        return result;
    }

    /**
     * OpenCLI 配置类
     */
    public static class OpenCLIConfig {
        /** 是否启用 OpenCLI */
        private boolean enabled = false;
        /** opencli 命令路径，默认 "opencli"（如果在全局 PATH 中） */
        private String command = "opencli";
        /** 命令执行超时秒数 */
        private int timeout = 30;
        /** 默认返回结果条数 */
        private int defaultLimit = 5;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getCommand() {
            return command;
        }

        public void setCommand(String command) {
            this.command = command;
        }

        public int getTimeout() {
            return timeout;
        }

        public void setTimeout(int timeout) {
            this.timeout = timeout;
        }

        public int getDefaultLimit() {
            return defaultLimit;
        }

        public void setDefaultLimit(int defaultLimit) {
            this.defaultLimit = defaultLimit;
        }
    }
}
