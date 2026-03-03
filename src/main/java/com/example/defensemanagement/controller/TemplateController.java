package com.example.defensemanagement.controller;

import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpSession;

/**
 * 超级管理员或院系管理员上传 Word 模板。
 * - 超级管理员：上传到全局目录 templates/
 * - 院系管理员：上传到院系目录 templates/dept_{deptId}/，仅覆盖本院系的模板
 */
@RestController
@RequestMapping("/admin/template")
public class TemplateController {

    @Autowired
    private FileStorageService fileStorageService;

    @PostMapping("/upload")
    public String uploadTemplate(@RequestParam String templateKey,
                                 @RequestParam("file") MultipartFile file,
                                 HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "error:权限不足";
        }
        String roleName = currentUser.getRole() != null ? currentUser.getRole().getName() : null;
        if (!"SUPER_ADMIN".equals(roleName) && !"DEPT_ADMIN".equals(roleName)) {
            return "error:权限不足";
        }

        // P5: 文件类型校验，只允许上传 .docx 文件
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".docx")) {
            return "error:只允许上传 .docx 格式的Word文档";
        }

        // P2: 院系隔离——院系管理员只能覆盖本院系的模板，超级管理员上传全局模板
        String subDir;
        if ("DEPT_ADMIN".equals(roleName)) {
            Long deptId = currentUser.getDepartmentId();
            if (deptId == null) {
                return "error:当前账号未绑定院系，无法上传模板";
            }
            subDir = "templates/dept_" + deptId;
        } else {
            subDir = "templates";
        }

        String path = fileStorageService.save(file, subDir, templateKey);
        return path;
    }
}
