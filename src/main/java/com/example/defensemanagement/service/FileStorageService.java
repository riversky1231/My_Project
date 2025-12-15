package com.example.defensemanagement.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    /**
     * 保存文件到指定子目录，返回相对路径。
     */
    String save(MultipartFile file, String subDir, String filename);
}

