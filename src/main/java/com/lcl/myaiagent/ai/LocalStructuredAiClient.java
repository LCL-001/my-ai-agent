package com.lcl.myaiagent.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lcl.myaiagent.common.ErrorCode;
import com.lcl.myaiagent.exception.BusinessException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.function.Predicate;

@Component
public class LocalStructuredAiClient {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    public LocalStructuredAiClient(ChatClient.Builder chatClientBuilder, ObjectMapper objectMapper) {
        this.chatClient = chatClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    public <T> T generate(String systemPrompt, String userPrompt, Class<T> responseType, Predicate<T> validator) {
        String prompt = userPrompt;
        for (int attempt = 0; attempt < 2; attempt++) {
            final String content;
            try {
                content = chatClient.prompt().system(systemPrompt).user(prompt).call().content();
            } catch (Exception exception) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "本地模型不可用，请确认 Ollama 已启动后重试");
            }
            try {
                T response = objectMapper.readValue(extractJson(content), responseType);
                if (validator.test(response)) {
                    return response;
                }
            } catch (JsonProcessingException ignored) {
                // The repair prompt below asks the model to return strict JSON once more.
            }
            prompt = userPrompt + "\n\n上一次输出无法通过格式或引用校验。只返回符合要求的 JSON，不要 Markdown、解释或额外文本。";
        }
        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "本地模型未返回可验证的结构化结果，请稍后重试");
    }

    private String extractJson(String content) {
        if (content == null) {
            throw new IllegalArgumentException("empty model output");
        }
        String trimmed = content.trim();
        if (trimmed.startsWith("```")) {
            int firstLineEnd = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstLineEnd >= 0 && lastFence > firstLineEnd) {
                trimmed = trimmed.substring(firstLineEnd + 1, lastFence).trim();
            }
        }
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start < 0 || end < start) {
            throw new IllegalArgumentException("JSON object not found");
        }
        return trimmed.substring(start, end + 1);
    }
}
