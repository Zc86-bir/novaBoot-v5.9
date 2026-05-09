package org.jeecg.modules.airag.app.handler;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.service.tool.ToolExecutor;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.util.oConvertUtils;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;
import java.util.*;

/**
 * SearXNG 免费搜索工具构建器
 * SearXNG 是开源元搜索引擎，可自建或使用公共实例
 * GitHub: https://github.com/searxng/searxng
 * 公共实例列表: https://searx.space
 *
 * @author Claude
 * @date 2026-05-09
 */
@Slf4j
public class SearXNGSearchToolBuilder {

    /**
     * 构建 SearXNG 搜索工具
     *
     * @param config SearXNG 配置
     * @return 工具Map
     */
    public static Map<ToolSpecification, ToolExecutor> buildTools(SearXNGSearchConfig config) {
        Map<ToolSpecification, ToolExecutor> tools = new HashMap<>();
        if (config == null || oConvertUtils.isEmpty(config.getBaseUrl())) {
            log.warn("[SearXNG]未配置 SearXNG 地址，联网搜索不生效");
            return tools;
        }

        ToolSpecification spec = ToolSpecification.builder()
                .name("web_search")
                .description("搜索互联网上的最新信息。当你需要回答关于近期事件、新闻、天气、股价或其他实时信息的问题时使用此工具。返回搜索结果包含标题、摘要和链接。")
                .parameters(JsonObjectSchema.builder()
                        .addStringProperty("query", "搜索关键词或查询语句")
                        .addNumberProperty("num_results", "返回结果数量（可选，默认5条，最多10条）")
                        .required("query")
                        .build())
                .build();

        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(new org.springframework.http.client.SimpleClientHttpRequestFactory() {{
            setConnectTimeout(config.getConnectTimeout() != null ? config.getConnectTimeout() : 5000);
            setReadTimeout((config.getTimeout() != null ? config.getTimeout() : 15) * 1000);
        }});

        ToolExecutor executor = (toolExecutionRequest, memoryId) -> {
            try {
                JSONObject args = JSONObject.parseObject(toolExecutionRequest.arguments());
                String query = args.getString("query");
                int numResults = args.getInteger("num_results");
                if (numResults <= 0 || numResults > 10) {
                    numResults = config.getCount() != null ? config.getCount() : 5;
                }

                // 构建请求URL
                String baseUrl = config.getBaseUrl();
                if (!baseUrl.endsWith("/")) {
                    baseUrl += "/";
                }

                URI uri = UriComponentsBuilder.fromHttpUrl(baseUrl + "search")
                        .queryParam("q", query)
                        .queryParam("format", "json")
                        .queryParam("categories", "general")
                        .queryParam("pageno", "1")
                        .queryParam("language", config.getLanguage() != null ? config.getLanguage() : "zh-CN")
                        .build()
                        .toUri();

                HttpHeaders headers = new HttpHeaders();
                headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
                if (oConvertUtils.isNotEmpty(config.getApiKey())) {
                    headers.set("Authorization", "Bearer " + config.getApiKey());
                }

                HttpEntity<Void> request = new HttpEntity<>(headers);
                ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, request, String.class);

                if (response.getStatusCode() != HttpStatus.OK) {
                    return "搜索请求失败，HTTP状态码: " + response.getStatusCodeValue();
                }

                // 解析响应
                JSONObject result = JSONObject.parseObject(response.getBody());
                JSONArray results = result.getJSONArray("results");
                if (results == null || results.isEmpty()) {
                    return "未找到相关搜索结果。";
                }

                // 提取结果
                StringBuilder sb = new StringBuilder();
                int count = Math.min(numResults, results.size());
                for (int i = 0; i < count; i++) {
                    JSONObject item = results.getJSONObject(i);
                    sb.append(i + 1).append(". ").append(item.getString("title")).append("\n");
                    if (oConvertUtils.isNotEmpty(item.getString("content"))) {
                        sb.append("   ").append(item.getString("content")).append("\n");
                    }
                    if (oConvertUtils.isNotEmpty(item.getString("url"))) {
                        sb.append("   链接: ").append(item.getString("url")).append("\n");
                    }
                    sb.append("\n");
                }

                return "搜索结果（共 " + results.size() + " 条，显示前 " + count + " 条）：\n\n" + sb.toString();

            } catch (Exception e) {
                log.error("[SearXNG]搜索失败: {}", e.getMessage(), e);
                return "搜索执行失败: " + e.getMessage();
            }
        };

        tools.put(spec, executor);
        log.info("[SearXNG]搜索工具已配置，实例: {}", config.getBaseUrl());
        return tools;
    }

    /**
     * SearXNG 搜索配置
     */
    public static class SearXNGSearchConfig {
        /** SearXNG 实例地址，例如 https://searx.example.org/ */
        private String baseUrl;
        /** API Key（可选，部分实例可能需要） */
        private String apiKey;
        /** 默认返回结果条数 */
        private Integer count = 5;
        /** 请求超时秒数 */
        private Integer timeout = 15;
        /** 连接超时毫秒数 */
        private Integer connectTimeout = 5000;
        /** 搜索语言，默认 zh-CN 中文 */
        private String language = "zh-CN";

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public Integer getCount() {
            return count;
        }

        public void setCount(Integer count) {
            this.count = count;
        }

        public Integer getTimeout() {
            return timeout;
        }

        public void setTimeout(Integer timeout) {
            this.timeout = timeout;
        }

        public Integer getConnectTimeout() {
            return connectTimeout;
        }

        public void setConnectTimeout(Integer connectTimeout) {
            this.connectTimeout = connectTimeout;
        }

        public String getLanguage() {
            return language;
        }

        public void setLanguage(String language) {
            this.language = language;
        }
    }
}
