package com.example.defensemanagement.service;

public interface AiCommentService {

    /**
     * 基于提示词与上下文生成评语。当前为占位实现，可根据实际 API 对接。
     * @param promptTemplateKey 配置中的模板 key（如 PAPER_PROMPT_TEMPLATE / DESIGN_PROMPT_TEMPLATE）
     * @param context 上下文（摘要、评分要点等）
     * @return 生成的评语
     */
    String generateComment(String promptTemplateKey, String context);
}

