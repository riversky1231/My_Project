package com.example.defensemanagement.service.impl;

import com.example.defensemanagement.service.AiCommentService;
import com.example.defensemanagement.service.ConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 调用通义千问 DashScope 文本生成接口。
 * API: https://dashscope.aliyun.com/api/v1/services/aigc/text-generation/generation
 */
@Service
public class AiCommentServiceImpl implements AiCommentService {

    private static final String DASH_SCOPE_URL = "https://dashscope.aliyun.com/api/v1/services/aigc/text-generation/generation";
    private static final String DEFAULT_MODEL = "qwen-turbo";

    @Autowired
    private ConfigService configService;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    @SuppressWarnings("unchecked")
    public String generateComment(String promptTemplateKey, String context) {
        String template = configService.getPromptTemplate(promptTemplateKey);
        String apiKey = configService.getConfigValue(ConfigServiceImpl.KEY_QWEN_API_KEY);

        if (!StringUtils.hasText(template)) {
            return "【提示】尚未配置评语提示词模板，无法调用大模型。";
        }
        if (!StringUtils.hasText(apiKey)) {
            return "【提示】尚未配置大模型 API Key，无法调用大模型。";
        }

        String prompt = template + "\n上下文：" + (context == null ? "" : context);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);

            Map<String, Object> body = new HashMap<>();
            body.put("model", DEFAULT_MODEL);
            Map<String, Object> input = new HashMap<>();
            Map<String, Object> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", prompt);
            input.put("messages", java.util.Arrays.asList(message));
            body.put("input", input);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            Map<String, Object> resp = restTemplate.postForObject(DASH_SCOPE_URL, entity, Map.class);
            if (resp == null) {
                return "【提示】调用大模型失败：无响应";
            }

            // DashScope 返回格式参考 openai 风格 choices[0].message.content
            Object output = resp.get("output");
            if (output instanceof Map) {
                Object choicesObj = ((Map<?, ?>) output).get("choices");
                if (choicesObj instanceof List && !((List<?>) choicesObj).isEmpty()) {
                    Object first = ((List<?>) choicesObj).get(0);
                    if (first instanceof Map) {
                        Object messageObj = ((Map<?, ?>) first).get("message");
                        if (messageObj instanceof Map) {
                            Object contentObj = ((Map<?, ?>) messageObj).get("content");
                            if (contentObj != null) {
                                return contentObj.toString();
                            }
                        }
                    }
                }
            }
            return "【提示】调用大模型成功但未获取到内容";
        } catch (Exception e) {
            return "【提示】调用大模型出错：" + e.getMessage();
        }
    }
}

