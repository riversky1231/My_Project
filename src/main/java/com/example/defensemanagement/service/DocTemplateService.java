package com.example.defensemanagement.service;

import java.util.Map;

public interface DocTemplateService {

    /**
        * 基于 docx 模板生成文档，支持简单文本占位符替换与图片占位插入。
        * @param templatePath classpath 下模板路径，如 templates/docx/paper-score.docx
        * @param placeholders 文本占位符映射，形如 {{NAME}} -> 张三
        * @param imageBytesMap 图片占位符映射，形如 {{SIGN_TEACHER}} -> byte[]
        * @return 生成后的 docx 字节
        */
    byte[] renderDoc(String templatePath,
                     Map<String, String> placeholders,
                     Map<String, byte[]> imageBytesMap);
}

