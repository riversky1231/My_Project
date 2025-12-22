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

    /**
     * 获取上传目录的绝对路径
     */
    private Path getBasePath() {
        Path basePath = Paths.get(baseDir);
        // 如果是相对路径，转换为相对于项目根目录的绝对路径
        if (!basePath.isAbsolute()) {
            // 获取项目根目录（通常是工作目录）
            String userDir = System.getProperty("user.dir");
            basePath = Paths.get(userDir, baseDir);
        }
        return basePath;
    }

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
            
            // 使用绝对路径
            Path basePath = getBasePath();
            Path dirPath = subDir == null || subDir.isEmpty() 
                ? basePath 
                : basePath.resolve(subDir);
            
            // 创建目录（如果不存在）
            Files.createDirectories(dirPath);
            
            Path target = dirPath.resolve(safeName);
            java.io.File targetFile = target.toFile();
            
            // 确保父目录存在
            java.io.File parentDir = targetFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            
            file.transferTo(targetFile);
            
            // 返回相对路径（相对于baseDir）
            Path relativePath = basePath.relativize(target);
            return relativePath.toString().replace("\\", "/");
        } catch (IOException e) {
            Path basePath = getBasePath();
            Path dirPath = subDir == null || subDir.isEmpty() 
                ? basePath 
                : basePath.resolve(subDir);
            throw new RuntimeException("保存文件失败: " + e.getMessage() + 
                " (目标目录: " + dirPath.toAbsolutePath() + ")", e);
        }
    }
}

