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
 * API: https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions
 * 使用兼容OpenAI格式的API端点
 */
@Service
public class AiCommentServiceImpl implements AiCommentService {

    private static final String DASH_SCOPE_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";
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

            // 构建请求体 - 通义千问DashScope API格式（兼容OpenAI格式）
            // 使用compatible-mode端点，直接使用messages格式
            Map<String, Object> body = new HashMap<>();
            body.put("model", DEFAULT_MODEL);

            // 构建messages数组
            java.util.List<Map<String, String>> messages = new java.util.ArrayList<>();
            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);
            messages.add(userMessage);

            // 直接使用messages，不需要input包装
            body.put("messages", messages);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            // 先获取String类型的响应，避免HttpMessageConverter错误
            // 这样可以处理text/plain格式的错误响应
            org.springframework.http.ResponseEntity<String> response = restTemplate.postForEntity(
                    DASH_SCOPE_URL, entity, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                // 如果状态码不是2xx，返回错误信息
                String errorBody = response.getBody() != null ? response.getBody() : "未知错误";
                return "【提示】调用大模型失败：HTTP " + response.getStatusCode() + " - " + errorBody;
            }

            String responseBody = response.getBody();
            if (responseBody == null || responseBody.trim().isEmpty()) {
                return "【提示】调用大模型失败：无响应";
            }

            // 手动解析JSON响应
            try {
                com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
                Map<String, Object> resp = objectMapper.readValue(responseBody, Map.class);

                // 检查是否有错误信息（OpenAI兼容格式或DashScope错误格式）
                if (resp.containsKey("error")) {
                    Object errorObj = resp.get("error");
                    if (errorObj instanceof Map) {
                        Object messageObj = ((Map<?, ?>) errorObj).get("message");
                        String errorMsg = messageObj != null ? messageObj.toString() : "未知错误";
                        return "【提示】调用大模型失败：" + errorMsg;
                    }
                }
                if (resp.containsKey("code") && resp.containsKey("message")) {
                    String errorCode = resp.get("code") != null ? resp.get("code").toString() : "";
                    String errorMsg = resp.get("message") != null ? resp.get("message").toString() : "";
                    return "【提示】调用大模型失败：[" + errorCode + "] " + errorMsg;
                }

                // OpenAI兼容格式：choices[0].message.content
                Object choicesObj = resp.get("choices");
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

                // 兼容旧格式：output.choices[0].message.content
                Object output = resp.get("output");
                if (output instanceof Map) {
                    Object outputChoicesObj = ((Map<?, ?>) output).get("choices");
                    if (outputChoicesObj instanceof List && !((List<?>) outputChoicesObj).isEmpty()) {
                        Object first = ((List<?>) outputChoicesObj).get(0);
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
                return "【提示】调用大模型成功但未获取到内容，响应：" + resp.toString();
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                // 如果无法解析为JSON，可能是错误响应
                return "【提示】调用大模型失败：无法解析响应 - " + responseBody + " (解析错误: " + e.getMessage() + ")";
            }
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            // 处理4xx错误
            String errorBody = e.getResponseBodyAsString();
            return "【提示】调用大模型失败：HTTP " + e.getStatusCode() + " - " +
                    (errorBody != null && !errorBody.isEmpty() ? errorBody : e.getMessage());
        } catch (org.springframework.web.client.HttpServerErrorException e) {
            // 处理5xx错误
            String errorBody = e.getResponseBodyAsString();
            return "【提示】调用大模型失败：HTTP " + e.getStatusCode() + " - " +
                    (errorBody != null && !errorBody.isEmpty() ? errorBody : e.getMessage());
        } catch (org.springframework.web.client.RestClientException e) {
            // 处理其他RestTemplate异常（包括HttpMessageConverter错误）
            return "【提示】调用大模型出错：" + e.getMessage() +
                    (e.getCause() != null ? " (原因: " + e.getCause().getMessage() + ")" : "");
        } catch (Exception e) {
            return "【提示】调用大模型出错：" + e.getMessage() +
                    (e.getCause() != null ? " (原因: " + e.getCause().getMessage() + ")" : "");
        }
    }
}
