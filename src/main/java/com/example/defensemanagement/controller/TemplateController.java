package com.example.defensemanagement.controller;

import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.service.AuthService;
import com.example.defensemanagement.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpSession;

/**
 * 超级管理员上传 Word 模板。
 */
@RestController
@RequestMapping("/admin/template")
public class TemplateController {

    @Autowired
    private AuthService authService;

    @Autowired
    private FileStorageService fileStorageService;

    @PostMapping("/upload")
    public String uploadTemplate(@RequestParam String templateKey,
                                 @RequestParam("file") MultipartFile file,
                                 HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null || !authService.hasPermission(currentUser, "SUPER_ADMIN_ACCESS")) {
            return "error:权限不足";
        }
        // 保存到 uploads/templates/
        String path = fileStorageService.save(file, "templates", templateKey);
        return path;
    }
}

