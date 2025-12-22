package com.example.defensemanagement.service.impl;

import com.example.defensemanagement.service.DocTemplateService;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Service
public class DocTemplateServiceImpl implements DocTemplateService {

    @Override
    public byte[] renderDoc(String templatePath, Map<String, String> placeholders, Map<String, byte[]> imageBytesMap) {
        try (InputStream is = resolve(templatePath);
             XWPFDocument document = new XWPFDocument(is);
             ByteArrayOutputStream os = new ByteArrayOutputStream()) {

            if (placeholders != null && !placeholders.isEmpty()) {
                replaceInParagraphs(document.getParagraphs(), placeholders);
                for (XWPFTable table : document.getTables()) {
                    table.getRows().forEach(row -> row.getTableCells().forEach(cell ->
                            replaceInParagraphs(cell.getParagraphs(), placeholders)));
                }
            }

            if (imageBytesMap != null && !imageBytesMap.isEmpty()) {
                insertImages(document, imageBytesMap);
            }

            document.write(os);
            return os.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("生成文档失败: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("null")
    private InputStream resolve(String path) throws Exception {
        Resource res = null;
        if (path.startsWith("classpath:")) {
            res = new ClassPathResource(path.substring("classpath:".length()));
        } else {
            // 先尝试作为文件系统路径
            res = new FileSystemResource(path);
            if (!res.exists()) {
                // 如果文件系统路径不存在，尝试作为classpath资源
                res = new ClassPathResource(path);
            }
        }
        if (res == null || !res.exists()) {
            String templateKey = path.contains("paper-score") ? "paper-score" : 
                  path.contains("design-score") ? "design-score" :
                  path.contains("paper-grade") ? "paper-grade" :
                  path.contains("design-grade") ? "design-grade" :
                  path.contains("paper-process") ? "paper-process" :
                  path.contains("design-process") ? "design-process" :
                  path.contains("group-summary") ? "group-summary" : "未知";
            String errorMsg = String.format(
                "模板文件不存在: %s。请以超级管理员身份登录，在\"系统设置\"->\"模板管理\"中上传对应的Word模板文件（模板key: %s.docx）。",
                path, templateKey);
            throw new IllegalArgumentException(errorMsg);
        }
        return res.getInputStream();
    }

    private void replaceInParagraphs(List<XWPFParagraph> paragraphs, Map<String, String> placeholders) {
        for (XWPFParagraph para : paragraphs) {
            String text = para.getText();
            boolean changed = false;
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                String key = entry.getKey();
                String val = entry.getValue();
                if (!StringUtils.hasText(key) || val == null) continue;
                if (text.contains(key)) {
                    text = text.replace(key, val);
                    changed = true;
                }
            }
            if (changed) {
                // 清空原 runs，重新写入
                int runCount = para.getRuns().size();
                for (int i = runCount - 1; i >= 0; i--) {
                    para.removeRun(i);
                }
                XWPFRun run = para.createRun();
                run.setText(text, 0);
            }
        }
    }

    private void insertImages(XWPFDocument document, Map<String, byte[]> imageBytesMap) {
        for (XWPFParagraph para : document.getParagraphs()) {
            replaceImageInParagraph(para, imageBytesMap);
        }
        for (XWPFTable table : document.getTables()) {
            table.getRows().forEach(row -> row.getTableCells().forEach(cell ->
                    cell.getParagraphs().forEach(p -> replaceImageInParagraph(p, imageBytesMap))));
        }
    }

    private void replaceImageInParagraph(XWPFParagraph para, Map<String, byte[]> imageBytesMap) {
        String text = para.getText();
        for (Map.Entry<String, byte[]> entry : imageBytesMap.entrySet()) {
            String key = entry.getKey();
            byte[] bytes = entry.getValue();
            if (text.contains(key) && bytes != null) {
                // 清空文字
                int runCount = para.getRuns().size();
                for (int i = runCount - 1; i >= 0; i--) {
                    para.removeRun(i);
                }
                try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes)) {
                    XWPFRun run = para.createRun();
                    run.addPicture(bais, XWPFDocument.PICTURE_TYPE_PNG, "image", Units.toEMU(120), Units.toEMU(50));
                } catch (Exception e) {
                    throw new RuntimeException("插入图片失败: " + e.getMessage(), e);
                }
            }
        }
    }
}

