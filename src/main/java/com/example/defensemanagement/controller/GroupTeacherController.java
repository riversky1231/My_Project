package com.example.defensemanagement.controller;

import com.example.defensemanagement.entity.DefenseGroup;
import com.example.defensemanagement.entity.DefenseGroupTeacher;
import com.example.defensemanagement.entity.Teacher;
import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.mapper.DefenseGroupMapper;
import com.example.defensemanagement.mapper.DefenseGroupTeacherMapper;
import com.example.defensemanagement.service.AuthService;
import com.example.defensemanagement.service.TeacherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.List;

/**
 * 院系管理员：小组-教师分配与组长设置
 */
@RestController
@RequestMapping("/department/group")
public class GroupTeacherController {

    @Autowired
    private AuthService authService;

    @Autowired
    private TeacherService teacherService;

    @Autowired
    private DefenseGroupMapper defenseGroupMapper;

    @Autowired
    private DefenseGroupTeacherMapper defenseGroupTeacherMapper;

    private User requireDeptAdmin(HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null || !authService.hasPermission(currentUser, "MANAGE_TEACHERS")) {
            return null;
        }
        return currentUser;
    }

    @GetMapping("/teachers")
    public List<Teacher> listDepartmentTeachers(HttpSession session) {
        User u = requireDeptAdmin(session);
        if (u == null) return java.util.Collections.emptyList();
        // super admin can view all, dept admin only own dept
        if (u.getRole() != null && "SUPER_ADMIN".equals(u.getRole().getName())) {
            return teacherService.findByDepartmentId(null);
        }
        return teacherService.findByDepartmentId(u.getDepartmentId());
    }

    @GetMapping("/{groupId}/teachers")
    public List<DefenseGroupTeacher> listGroupTeachers(@PathVariable Long groupId, HttpSession session) {
        User u = requireDeptAdmin(session);
        if (u == null) return java.util.Collections.emptyList();
        DefenseGroup g = defenseGroupMapper.findById(groupId);
        if (g == null) return java.util.Collections.emptyList();
        return defenseGroupTeacherMapper.findByGroupId(groupId);
    }

    @PostMapping("/{groupId}/teacher/assign")
    public String assignTeacher(@PathVariable Long groupId, @RequestParam Long teacherId, HttpSession session) {
        User u = requireDeptAdmin(session);
        if (u == null) return "error:权限不足";
        DefenseGroup g = defenseGroupMapper.findById(groupId);
        if (g == null) return "error:小组不存在";
        Teacher t = teacherService.findById(teacherId);
        if (t == null) return "error:教师不存在";
        if (u.getRole() != null && "DEPT_ADMIN".equals(u.getRole().getName())
                && u.getDepartmentId() != null && !u.getDepartmentId().equals(t.getDepartmentId())) {
            return "error:只能分配本院系教师";
        }
        
        // 检查该教师是否已经属于其他小组
        DefenseGroupTeacher existing = defenseGroupTeacherMapper.findByTeacherId(teacherId);
        if (existing != null && !existing.getGroupId().equals(groupId)) {
            // 教师已经属于其他小组，返回错误信息
            DefenseGroup existingGroup = defenseGroupMapper.findById(existing.getGroupId());
            String groupName = existingGroup != null ? existingGroup.getName() : "其他小组";
            return "error:该教师已经属于" + groupName + "，一个教师不能加入两个小组";
        }
        
        // 如果教师已经在当前小组，直接返回成功（避免重复插入）
        if (existing != null && existing.getGroupId().equals(groupId)) {
            return "success";
        }
        
        defenseGroupTeacherMapper.insert(groupId, teacherId, 0);
        return "success";
    }

    @DeleteMapping("/{groupId}/teacher/{teacherId}")
    public String removeTeacher(@PathVariable Long groupId, @PathVariable Long teacherId, HttpSession session) {
        User u = requireDeptAdmin(session);
        if (u == null) return "error:权限不足";
        defenseGroupTeacherMapper.delete(groupId, teacherId);
        return "success";
    }

    @PostMapping("/{groupId}/leader/set")
    public String setLeader(@PathVariable Long groupId, @RequestParam Long teacherId, HttpSession session) {
        User u = requireDeptAdmin(session);
        if (u == null) return "error:权限不足";
        DefenseGroup g = defenseGroupMapper.findById(groupId);
        if (g == null) return "error:小组不存在";
        Teacher t = teacherService.findById(teacherId);
        if (t == null) return "error:教师不存在";
        if (u.getRole() != null && "DEPT_ADMIN".equals(u.getRole().getName())
                && u.getDepartmentId() != null && !u.getDepartmentId().equals(t.getDepartmentId())) {
            return "error:只能设置本院系教师为组长";
        }
        // ensure teacher is assigned to this group, then mark leader
        defenseGroupTeacherMapper.insert(groupId, teacherId, 0);
        defenseGroupTeacherMapper.clearLeader(groupId);
        defenseGroupTeacherMapper.setLeader(groupId, teacherId);
        return "success";
    }
}


