package com.example.defensemanagement.controller;

import com.example.defensemanagement.entity.Teacher;
import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.service.ConfigService;
import com.example.defensemanagement.mapper.TeacherMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 登录提醒接口：登录后检查是否有待办事项需要弹窗提醒
 */
@RestController
@RequestMapping("/api")
public class ReminderController {

    @Autowired
    private ConfigService configService;

    @Autowired
    private TeacherMapper teacherMapper;

    /**
     * 获取当前用户的待办提醒列表
     * GET /api/reminders
     * 返回: { "reminders": [ { "type": "...", "message": "..." } ] }
     */
    @GetMapping("/reminders")
    public Map<String, Object> getReminders(HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, String>> reminders = new ArrayList<>();

        // 确定当前用户角色
        String userType = (String) session.getAttribute("userType");
        User currentUser = (User) session.getAttribute("currentUser");
        Teacher currentTeacher = (Teacher) session.getAttribute("currentTeacher");

        String roleName = null;
        if (currentUser != null && currentUser.getRole() != null) {
            roleName = currentUser.getRole().getName();
        }
        if ("TEACHER".equals(userType) && currentTeacher != null) {
            roleName = "TEACHER";
        }

        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        // === 志愿互选截止时间提醒（学生 + 教师） ===
        if ("STUDENT".equals(roleName) || "TEACHER".equals(roleName) || "DEFENSE_LEADER".equals(roleName)) {
            String volunteerDeadline = configService.getConfigValue("VOLUNTEER_DEADLINE");
            if (volunteerDeadline != null && !volunteerDeadline.trim().isEmpty()) {
                try {
                    LocalDateTime deadline = LocalDateTime.parse(volunteerDeadline.trim(), fmt);
                    if (now.isBefore(deadline)) {
                        Map<String, String> reminder = new HashMap<>();
                        reminder.put("type", "volunteer");
                        reminder.put("message", "师生互选截止时间为 " + volunteerDeadline.trim() + "，请尽快完成互选！");
                        reminder.put("deadline", volunteerDeadline.trim());
                        reminders.add(reminder);
                    }
                } catch (Exception e) {
                    // 格式错误忽略
                }
            }
        }

        // === 大组打分截止时间提醒（教师） ===
        if ("TEACHER".equals(roleName) || "DEFENSE_LEADER".equals(roleName)) {
            String lgDeadline = configService.getConfigValue("LARGE_GROUP_DEADLINE");
            String lgArchived = configService.getConfigValue("LARGE_GROUP_ARCHIVED");
            if (!"1".equals(lgArchived) && lgDeadline != null && !lgDeadline.trim().isEmpty()) {
                try {
                    LocalDateTime deadline = LocalDateTime.parse(lgDeadline.trim(), fmt);
                    if (now.isBefore(deadline)) {
                        Map<String, String> reminder = new HashMap<>();
                        reminder.put("type", "largegroup");
                        reminder.put("message", "大组打分截止时间为 " + lgDeadline.trim() + "，请尽快完成打分！");
                        reminder.put("deadline", lgDeadline.trim());
                        reminders.add(reminder);
                    }
                } catch (Exception e) {
                    // 格式错误忽略
                }
            }
        }

        result.put("reminders", reminders);
        return result;
    }
}
