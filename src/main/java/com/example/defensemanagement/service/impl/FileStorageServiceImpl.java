package com.example.defensemanagement.service.impl;

import com.example.defensemanagement.service.FileStorageService;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    @Value("${app.upload.base-dir:uploads}")
    private String baseDir;

    @Override
    @SuppressWarnings("null")
    public String save(MultipartFile file, String subDir, String filename) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件为空");
        }
        try {
            String ext = FilenameUtils.getExtension(file.getOriginalFilename());
            String safeName = StringUtils.hasText(filename) ? filename : UUID.randomUUID().toString();
            if (StringUtils.hasText(ext)) {
                safeName = safeName + "." + ext;
            }
            Path dirPath = Paths.get(baseDir, subDir == null ? "" : subDir);
            Files.createDirectories(dirPath);
            Path target = dirPath.resolve(safeName);
            java.io.File targetFile = target.toFile();
            file.transferTo(targetFile);
            // 返回相对路径
            return dirPath.relativize(target).toString().replace("\\", "/");
        } catch (IOException e) {
            throw new RuntimeException("保存文件失败: " + e.getMessage(), e);
        }
    }
}

