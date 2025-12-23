package com.example.defensemanagement.controller;

import com.example.defensemanagement.entity.Teacher;
import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.mapper.DefenseLeaderMapper;
import com.example.defensemanagement.mapper.TeacherMapper;
import com.example.defensemanagement.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 教师/院系管理员/系主任/超级管理员签名上传。
 */
@RestController
@RequestMapping("/signature")
public class SignatureController {

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private DefenseLeaderMapper defenseLeaderMapper;

    @Autowired
    private TeacherMapper teacherMapper;

    @PostMapping("/upload")
    public String uploadSignature(@RequestParam("file") org.springframework.web.multipart.MultipartFile file,
                                  HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        Teacher currentTeacher = (Teacher) session.getAttribute("currentTeacher");
        if (currentUser == null && currentTeacher == null) {
            return "error:未登录";
        }
        String filename = currentUser != null ? "user_" + currentUser.getId() : "teacher_" + currentTeacher.getId();
        String path = fileStorageService.save(file, "signatures", filename);
        return path;
    }

    /**
     * 院系管理员上传系主任签名
     * @param teacherId 系主任的教师ID
     * @param file 签名图片文件
     * @param session HTTP会话
     * @return 上传结果
     */
    @PostMapping("/upload/defenseLeader")
    public String uploadDefenseLeaderSignature(@RequestParam("teacherId") Long teacherId,
                                               @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
                                               HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "error:未登录";
        }

        // 检查权限：只有院系管理员和超级管理员可以上传系主任签名
        String roleName = currentUser.getRole() != null ? currentUser.getRole().getName() : null;
        if (!"SUPER_ADMIN".equals(roleName) && !"DEPT_ADMIN".equals(roleName)) {
            return "error:权限不足，只有院系管理员和超级管理员可以上传系主任签名";
        }

        // 验证系主任是否存在
        Teacher teacher = teacherMapper.findById(teacherId);
        if (teacher == null) {
            return "error:教师不存在";
        }

        // 如果是院系管理员，检查系主任是否属于同一院系
        if ("DEPT_ADMIN".equals(roleName)) {
            if (!currentUser.getDepartmentId().equals(teacher.getDepartmentId())) {
                return "error:只能管理本院系的系主任签名";
            }
        }

        // 保存签名文件，使用 teacher_教师ID 作为文件名
        String filename = "teacher_" + teacherId;
        String path = fileStorageService.save(file, "signatures", filename);
        return "success:" + path;
    }

    /**
     * 获取院系管理员的系主任列表（用于签名管理）
     * @param session HTTP会话
     * @return 系主任列表
     */
    @GetMapping("/defenseLeaders")
    @ResponseBody
    public Map<String, Object> getDefenseLeaders(HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "未登录");
            return error;
        }

        String roleName = currentUser.getRole() != null ? currentUser.getRole().getName() : null;
        if (!"SUPER_ADMIN".equals(roleName) && !"DEPT_ADMIN".equals(roleName)) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "权限不足");
            return error;
        }

        // 获取当前年份
        int currentYear = java.time.LocalDate.now().getYear();
        
        // 如果是院系管理员，只获取本院系的系主任
        List<com.example.defensemanagement.entity.DefenseLeader> leaders;
        if ("DEPT_ADMIN".equals(roleName)) {
            leaders = defenseLeaderMapper.findByDepartmentIdAndYear(currentUser.getDepartmentId(), currentYear);
        } else {
            // 超级管理员获取所有系主任
            leaders = defenseLeaderMapper.findByYear(currentYear);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("leaders", leaders);
        return result;
    }
}

