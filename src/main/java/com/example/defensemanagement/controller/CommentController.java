package com.example.defensemanagement.controller;

import com.example.defensemanagement.entity.Student;
import com.example.defensemanagement.entity.StudentComment;
import com.example.defensemanagement.entity.Teacher;
import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.service.AiCommentService;
import com.example.defensemanagement.mapper.StudentCommentMapper;
import com.example.defensemanagement.mapper.StudentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/defense/comment")
public class CommentController {

    @Autowired
    private StudentCommentMapper studentCommentMapper;

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private AiCommentService aiCommentService;

    /**
     * 获取小组内所有学生的评语列表
     * 所有角色都可以使用：超级管理员、院系管理员、答辩组长、教师
     * GET /defense/comment/group/{groupId}?year=2024
     */
    @GetMapping("/group/{groupId}")
    @ResponseBody
    public List<StudentComment> getGroupComments(@PathVariable Long groupId,
            @RequestParam Integer year,
            HttpSession session) {
        // 检查是否已登录
        User currentUser = (User) session.getAttribute("currentUser");
        Teacher currentTeacher = (Teacher) session.getAttribute("currentTeacher");

        if (currentUser == null && currentTeacher == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录");
        }

        // 所有已登录用户都可以查看评语列表
        return studentCommentMapper.findByGroupIdAndYear(groupId, year);
    }

    /**
     * 生成学生评语（基于AI）
     * 所有角色都可以使用：超级管理员、院系管理员、答辩组长、教师
     * POST /defense/comment/generate
     * request body: { "studentId": 1, "year": 2024 }
     */
    @PostMapping("/generate")
    @ResponseBody
    public Map<String, Object> generateComment(@RequestBody Map<String, Object> request, HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        Teacher currentTeacher = (Teacher) session.getAttribute("currentTeacher");

        if (currentUser == null && currentTeacher == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录");
        }

        Long studentId = Long.valueOf(request.get("studentId").toString());
        Integer year = Integer.valueOf(request.get("year").toString());

        Student student = studentMapper.findById(studentId);
        if (student == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "学生不存在");
        }

        // 权限检查：所有已登录用户都可以生成评语
        // 超级管理员和院系管理员可以查看所有学生
        // 教师和答辩组长可以查看自己相关的学生（这里不做限制，允许所有角色生成评语）

        // 确定使用哪个提示词模板
        String promptKey = "PAPER".equals(student.getDefenseType()) ? "PAPER_PROMPT_TEMPLATE"
                : "DESIGN_PROMPT_TEMPLATE";

        // 构建上下文
        String context = "学生姓名：" + (student.getName() != null ? student.getName() : "") +
                "\n题目：" + (student.getTitle() != null ? student.getTitle() : "") +
                "\n摘要：" + (student.getSummary() != null ? student.getSummary() : "");

        // 调用AI生成评语
        String generatedComment = aiCommentService.generateComment(promptKey, context);

        Map<String, Object> result = new HashMap<>();
        result.put("comment", generatedComment);
        result.put("studentId", studentId);
        result.put("year", year);
        return result;
    }

    /**
     * 保存或更新学生评语
     * 所有角色都可以使用：超级管理员、院系管理员、答辩组长、教师
     * POST /defense/comment/save
     * request body: { "studentId": 1, "year": 2024, "content": "评语内容" }
     */
    @PostMapping("/save")
    @ResponseBody
    public String saveComment(@RequestBody Map<String, Object> request, HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        Teacher currentTeacher = (Teacher) session.getAttribute("currentTeacher");

        if (currentUser == null && currentTeacher == null) {
            return "error:未登录";
        }

        try {
            Long studentId = Long.valueOf(request.get("studentId").toString());
            Integer year = Integer.valueOf(request.get("year").toString());
            String content = (String) request.get("content");

            if (content == null || content.trim().isEmpty()) {
                return "error:评语内容不能为空";
            }

            // 验证学生是否存在
            Student student = studentMapper.findById(studentId);
            if (student == null) {
                return "error:学生不存在";
            }

            // 权限检查：所有已登录用户都可以保存评语
            // 超级管理员和院系管理员可以查看所有学生
            // 教师和答辩组长可以查看自己相关的学生（这里不做限制，允许所有角色保存评语）

            // 查找是否已存在
            StudentComment existing = studentCommentMapper.findByStudentIdAndYear(studentId, year);

            if (existing != null) {
                // 更新
                existing.setContent(content);
                studentCommentMapper.update(existing);
            } else {
                // 新建
                StudentComment comment = new StudentComment();
                comment.setStudentId(studentId);
                comment.setYear(year);
                comment.setContent(content);
                studentCommentMapper.insert(comment);
            }

            return "success";
        } catch (Exception e) {
            return "error:保存失败, " + e.getMessage();
        }
    }

    /**
     * 获取学生评语
     * GET /defense/comment/student/{studentId}?year=2024
     */
    @GetMapping("/student/{studentId}")
    @ResponseBody
    public StudentComment getStudentComment(@PathVariable Long studentId,
            @RequestParam Integer year,
            HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        Teacher currentTeacher = (Teacher) session.getAttribute("currentTeacher");

        if (currentUser == null && currentTeacher == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录");
        }

        return studentCommentMapper.findByStudentIdAndYear(studentId, year);
    }
}
