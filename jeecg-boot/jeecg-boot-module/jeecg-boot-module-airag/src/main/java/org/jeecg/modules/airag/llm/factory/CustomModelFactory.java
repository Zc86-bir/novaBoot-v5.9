package org.jeecg.modules.airag.llm.factory;

import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.anthropic.AnthropicStreamingChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiImageModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.ai.factory.AiModelOptions;
import org.jeecg.common.util.oConvertUtils;

import java.time.Duration;
import java.util.Map;

/**
 * 自定义 AI 模型工厂
 * 支持通过 OpenAI 兼容接口接入任意第三方模型（如 vLLM、LMStudio、SiliconFlow 等）
 * 也支持 Anthropic 兼容接口
 *
 * @author Zc86-bir
 * @date 2026-05-03
 */
@Slf4j
public class CustomModelFactory {

    /**
     * 协议类型
     */
    public static final String PROTOCOL_OPENAI = "openai";
    public static final String PROTOCOL_ANTHROPIC = "anthropic";

    /**
     * 创建聊天模型
     */
    public static ChatModel createChatModel(AiModelOptions options) {
        String protocol = getProtocol(options);
        if (PROTOCOL_ANTHROPIC.equalsIgnoreCase(protocol)) {
            return createAnthropicChatModel(options);
        }
        return createOpenAiChatModel(options);
    }

    /**
     * 创建流式聊天模型
     */
    public static StreamingChatModel createStreamingChatModel(AiModelOptions options) {
        String protocol = getProtocol(options);
        if (PROTOCOL_ANTHROPIC.equalsIgnoreCase(protocol)) {
            return createAnthropicStreamingChatModel(options);
        }
        return createOpenAiStreamingChatModel(options);
    }

    /**
     * 创建向量模型
     */
    public static EmbeddingModel createEmbeddingModel(AiModelOptions options) {
        return createOpenAiEmbeddingModel(options);
    }

    /**
     * 创建图像模型
     */
    public static ImageModel createImageModel(AiModelOptions options) {
        return createOpenAiImageModel(options);
    }

    private static String getProtocol(AiModelOptions options) {
        Map<String, Object> extraParams = options.getExtraParams();
        if (extraParams != null && extraParams.containsKey("protocol")) {
            return String.valueOf(extraParams.get("protocol"));
        }
        return PROTOCOL_OPENAI;
    }

    private static String normalizeBaseUrl(String baseUrl) {
        if (oConvertUtils.isEmpty(baseUrl)) {
            return "https://api.openai.com/v1";
        }
        if (baseUrl.endsWith("/v1") || baseUrl.endsWith("/v1/")) {
            return baseUrl;
        }
        return baseUrl.replaceAll("/+$", "") + "/v1";
    }

    private static Duration toDuration(Object timeout) {
        // 第三方端点（如 SiliconFlow）可能需要更长的响应时间，默认 180 秒
        int seconds = oConvertUtils.getInteger(timeout, 180);
        return Duration.ofSeconds(seconds);
    }

    // ==================== OpenAI 兼容协议 ====================

    private static ChatModel createOpenAiChatModel(AiModelOptions options) {
        String baseUrl = normalizeBaseUrl(options.getBaseUrl());
        Duration timeout = toDuration(options.getTimeout());

        OpenAiChatModel.OpenAiChatModelBuilder builder = OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(options.getApiKey())
                .modelName(options.getModelName())
                .timeout(timeout)
                .temperature(options.getTemperature())
                .topP(options.getTopP())
                .maxTokens(options.getMaxTokens())
                .presencePenalty(options.getPresencePenalty())
                .frequencyPenalty(options.getFrequencyPenalty());

        applyOpenAiExtraParams(builder, options.getExtraParams());
        return builder.build();
    }

    private static StreamingChatModel createOpenAiStreamingChatModel(AiModelOptions options) {
        String baseUrl = normalizeBaseUrl(options.getBaseUrl());
        Duration timeout = toDuration(options.getTimeout());

        OpenAiStreamingChatModel.OpenAiStreamingChatModelBuilder builder =
                OpenAiStreamingChatModel.builder()
                        .baseUrl(baseUrl)
                        .apiKey(options.getApiKey())
                        .modelName(options.getModelName())
                        .timeout(timeout)
                        .temperature(options.getTemperature())
                        .topP(options.getTopP())
                        .maxTokens(options.getMaxTokens())
                        .presencePenalty(options.getPresencePenalty())
                        .frequencyPenalty(options.getFrequencyPenalty());

        applyOpenAiStreamingExtraParams(builder, options.getExtraParams());
        return builder.build();
    }

    private static EmbeddingModel createOpenAiEmbeddingModel(AiModelOptions options) {
        String baseUrl = normalizeBaseUrl(options.getBaseUrl());
        Duration timeout = toDuration(options.getTimeout());

        OpenAiEmbeddingModel.OpenAiEmbeddingModelBuilder builder = OpenAiEmbeddingModel.builder()
                .baseUrl(baseUrl)
                .apiKey(options.getApiKey())
                .modelName(options.getModelName())
                .timeout(timeout);

        Map<String, Object> extraParams = options.getExtraParams();
        if (extraParams != null && extraParams.containsKey("dimensions")) {
            Object dim = extraParams.get("dimensions");
            if (dim instanceof Integer) {
                builder.dimensions((Integer) dim);
            }
        }
        return builder.build();
    }

    private static ImageModel createOpenAiImageModel(AiModelOptions options) {
        String baseUrl = normalizeBaseUrl(options.getBaseUrl());
        Duration timeout = toDuration(options.getTimeout());

        OpenAiImageModel.OpenAiImageModelBuilder builder = OpenAiImageModel.builder()
                .baseUrl(baseUrl)
                .apiKey(options.getApiKey())
                .modelName(options.getModelName())
                .timeout(timeout);

        if (options.getImageSize() != null) {
            builder.size(options.getImageSize());
        }
        return builder.build();
    }

    @SuppressWarnings("unchecked")
    private static void applyOpenAiExtraParams(OpenAiChatModel.OpenAiChatModelBuilder builder, Map<String, Object> extraParams) {
        if (extraParams == null) return;
        if (extraParams.containsKey("organization")) {
            builder.organizationId(String.valueOf(extraParams.get("organization")));
        }
        if (extraParams.containsKey("user")) {
            builder.user(String.valueOf(extraParams.get("user")));
        }
        if (extraParams.containsKey("stop") && extraParams.get("stop") instanceof java.util.List) {
            builder.stop((java.util.List<String>) extraParams.get("stop"));
        }
        if (extraParams.containsKey("seed")) {
            Object seed = extraParams.get("seed");
            if (seed instanceof Number) {
                builder.seed(((Number) seed).intValue());
            }
        }
        if (extraParams.containsKey("logitBias") && extraParams.get("logitBias") instanceof Map) {
            builder.logitBias((Map<String, Integer>) extraParams.get("logitBias"));
        }
    }

    @SuppressWarnings("unchecked")
    private static void applyOpenAiStreamingExtraParams(OpenAiStreamingChatModel.OpenAiStreamingChatModelBuilder builder, Map<String, Object> extraParams) {
        if (extraParams == null) return;
        if (extraParams.containsKey("organization")) {
            builder.organizationId(String.valueOf(extraParams.get("organization")));
        }
        if (extraParams.containsKey("user")) {
            builder.user(String.valueOf(extraParams.get("user")));
        }
        if (extraParams.containsKey("stop") && extraParams.get("stop") instanceof java.util.List) {
            builder.stop((java.util.List<String>) extraParams.get("stop"));
        }
        if (extraParams.containsKey("seed")) {
            Object seed = extraParams.get("seed");
            if (seed instanceof Number) {
                builder.seed(((Number) seed).intValue());
            }
        }
    }

    // ==================== Anthropic 兼容协议 ====================

    private static ChatModel createAnthropicChatModel(AiModelOptions options) {
        String baseUrl = normalizeAnthropicUrl(options.getBaseUrl());
        Duration timeout = toDuration(options.getTimeout());

        AnthropicChatModel.AnthropicChatModelBuilder builder = AnthropicChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(options.getApiKey())
                .modelName(options.getModelName())
                .timeout(timeout)
                .temperature(options.getTemperature())
                .topP(options.getTopP())
                .maxTokens(options.getMaxTokens());

        applyAnthropicExtraParams(builder, options.getExtraParams());
        return builder.build();
    }

    private static StreamingChatModel createAnthropicStreamingChatModel(AiModelOptions options) {
        String baseUrl = normalizeAnthropicUrl(options.getBaseUrl());
        Duration timeout = toDuration(options.getTimeout());

        AnthropicStreamingChatModel.AnthropicStreamingChatModelBuilder builder =
                AnthropicStreamingChatModel.builder()
                        .baseUrl(baseUrl)
                        .apiKey(options.getApiKey())
                        .modelName(options.getModelName())
                        .timeout(timeout)
                        .temperature(options.getTemperature())
                        .topP(options.getTopP())
                        .maxTokens(options.getMaxTokens());

        applyAnthropicExtraParams(builder, options.getExtraParams());
        return builder.build();
    }

    private static String normalizeAnthropicUrl(String baseUrl) {
        if (oConvertUtils.isEmpty(baseUrl)) {
            return "https://api.anthropic.com";
        }
        // Anthropic API 不需要 /v1 后缀
        return baseUrl.replaceAll("/+$", "");
    }

    private static void applyAnthropicExtraParams(Object builder, Map<String, Object> extraParams) {
        if (extraParams == null) return;
        // Anthropic 的 topK 参数
        if (extraParams.containsKey("topK")) {
            Object topK = extraParams.get("topK");
            if (topK instanceof Number) {
                int topKVal = ((Number) topK).intValue();
                if (builder instanceof AnthropicChatModel.AnthropicChatModelBuilder) {
                    ((AnthropicChatModel.AnthropicChatModelBuilder) builder).topK(topKVal);
                } else if (builder instanceof AnthropicStreamingChatModel.AnthropicStreamingChatModelBuilder) {
                    ((AnthropicStreamingChatModel.AnthropicStreamingChatModelBuilder) builder).topK(topKVal);
                }
            }
        }
    }
}
