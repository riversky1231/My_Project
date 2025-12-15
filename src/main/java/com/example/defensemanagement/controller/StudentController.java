package com.example.defensemanagement.controller;

import com.example.defensemanagement.entity.Student;
import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.entity.Teacher;
import com.example.defensemanagement.service.AuthService;
import com.example.defensemanagement.service.StudentService;
import com.example.defensemanagement.service.ConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.List;

@Controller
@RequestMapping("/department/student")
public class StudentController {

    @Autowired
    private StudentService studentService;

    @Autowired
    private AuthService authService;

    @Autowired
    private ConfigService configService;

    // 检查院系管理员权限的辅助方法
    private String checkDeptAdmin(HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null || !authService.hasPermission(currentUser, "MANAGE_STUDENTS")) {
            return "error:权限不足";
        }
        return null; // 权限通过
    }

    /**
     * 获取当前年份和院系的学生列表 (院系管理员功能)
     * GET /department/student/list
     */
    @GetMapping("/list")
    @ResponseBody
    public List<Student> getStudentsByDept(HttpSession session) {
        String permissionError = checkDeptAdmin(session);
        if (permissionError != null) {
            // 如果不是院系管理员，则只允许教师查看自己指导的学生
            Teacher currentTeacher = (Teacher) session.getAttribute("currentTeacher");
            if (currentTeacher != null) {
                Integer currentYear = configService.getCurrentDefenseYear();
                if (currentYear == null) {
                    throw new RuntimeException("请先设置当前答辩年份");
                }
                // 教师只能查看自己指导的学生
                return studentService.getStudentsByAdvisor(currentTeacher.getId(), currentYear);
            }
            throw new RuntimeException("权限不足或未登录");
        }

        User currentUser = (User) session.getAttribute("currentUser");
        Long departmentId = currentUser.getDepartmentId();
        Integer currentYear = configService.getCurrentDefenseYear();

        if (departmentId == null || currentYear == null) {
            throw new RuntimeException("请先设置当前答辩年份或配置院系信息");
        }

        // 假设 StudentMapper 中有一个 findByDepartmentAndYear 方法
        // 院系管理员管理自己系的学生
        return studentService.findByDepartmentAndYear(departmentId, currentYear);
    }

    /**
     * 保存或更新学生信息
     * POST /department/student/save
     */
    @PostMapping("/save")
    @ResponseBody
    public String saveStudent(@RequestBody Student student, HttpSession session) {
        String permissionError = checkDeptAdmin(session);
        if (permissionError != null) {
            return permissionError;
        }

        try {
            studentService.saveStudent(student);
            return "success";
        } catch (Exception e) {
            return "error:保存学生信息失败, " + e.getMessage();
        }
    }

    /**
     * 分配指导教师
     * POST /department/student/assign/advisor?studentId=1&teacherId=T001
     */
    @PostMapping("/assign/advisor")
    @ResponseBody
    public String assignAdvisor(@RequestParam Long studentId, @RequestParam Long teacherId, HttpSession session) {
        String permissionError = checkDeptAdmin(session);
        if (permissionError != null) {
            return permissionError;
        }

        try {
            studentService.assignAdvisor(studentId, teacherId);
            return "success";
        } catch (Exception e) {
            return "error:分配指导教师失败, " + e.getMessage();
        }
    }

    /**
     * 分配评阅人
     * POST /department/student/assign/reviewer?studentId=1&teacherId=T002
     */
    @PostMapping("/assign/reviewer")
    @ResponseBody
    public String assignReviewer(@RequestParam Long studentId, @RequestParam Long teacherId, HttpSession session) {
        String permissionError = checkDeptAdmin(session);
        if (permissionError != null) {
            return permissionError;
        }

        try {
            studentService.assignReviewer(studentId, teacherId);
            return "success";
        } catch (Exception e) {
            return "error:分配评阅人失败, " + e.getMessage();
        }
    }

    /**
     * 答辩分组功能：将学生分配到答辩小组
     * POST /department/student/assign/group?studentId=1&groupId=2
     */
    @PostMapping("/assign/group")
    @ResponseBody
    public String assignDefenseGroup(@RequestParam Long studentId, @RequestParam Long groupId, HttpSession session) {
        String permissionError = checkDeptAdmin(session);
        if (permissionError != null) {
            return permissionError;
        }

        try {
            studentService.assignDefenseGroup(studentId, groupId);
            return "success";
        } catch (Exception e) {
            return "error:分配答辩小组失败, " + e.getMessage();
        }
    }
}