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
import java.util.ArrayList;
import java.util.Comparator;
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
        if (text == null || text.isEmpty()) {
            return;
        }
        
        // 收集所有需要替换的占位符及其位置
        List<PlaceholderInfo> placeholders = new ArrayList<>();
        for (Map.Entry<String, byte[]> entry : imageBytesMap.entrySet()) {
            String key = entry.getKey();
            byte[] bytes = entry.getValue();
            if (bytes != null) {
                int pos = text.indexOf(key);
                while (pos >= 0) {
                    placeholders.add(new PlaceholderInfo(key, pos, bytes));
                    pos = text.indexOf(key, pos + key.length());
                }
            }
        }
        
        // 如果没有找到任何占位符，直接返回
        if (placeholders.isEmpty()) {
            return;
        }
        
        // 按位置排序
        placeholders.sort(Comparator.comparingInt(p -> p.position));
        
        // 清空段落
        int runCount = para.getRuns().size();
        for (int i = runCount - 1; i >= 0; i--) {
            para.removeRun(i);
        }
        
        // 按顺序重建段落：文本和图片交替
        int currentPos = 0;
        for (PlaceholderInfo ph : placeholders) {
            // 添加占位符前面的文本
            if (ph.position > currentPos) {
                String textBefore = text.substring(currentPos, ph.position);
                if (!textBefore.isEmpty()) {
                    XWPFRun textRun = para.createRun();
                    textRun.setText(textBefore, 0);
                }
            }
            
            // 添加图片
            try (ByteArrayInputStream bais = new ByteArrayInputStream(ph.imageBytes)) {
                XWPFRun imageRun = para.createRun();
                imageRun.addPicture(bais, XWPFDocument.PICTURE_TYPE_PNG, "signature", Units.toEMU(120), Units.toEMU(50));
            } catch (Exception e) {
                throw new RuntimeException("插入图片失败: " + e.getMessage(), e);
            }
            
            currentPos = ph.position + ph.key.length();
        }
        
        // 添加最后一个占位符后面的文本
        if (currentPos < text.length()) {
            String textAfter = text.substring(currentPos);
            if (!textAfter.isEmpty()) {
                XWPFRun textRun = para.createRun();
                textRun.setText(textAfter, 0);
            }
        }
    }
    
    /**
     * 占位符信息类，用于记录占位符的位置和对应的图片数据
     */
    private static class PlaceholderInfo {
        String key;
        int position;
        byte[] imageBytes;
        
        PlaceholderInfo(String key, int position, byte[] imageBytes) {
            this.key = key;
            this.position = position;
            this.imageBytes = imageBytes;
        }
    }
}

