package com.example.defensemanagement.controller;

import com.example.defensemanagement.entity.DefenseGroup;
import com.example.defensemanagement.entity.DefenseGroupTeacher;
import com.example.defensemanagement.entity.Teacher;
import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.mapper.DefenseGroupMapper;
import com.example.defensemanagement.mapper.DefenseGroupTeacherMapper;
import com.example.defensemanagement.mapper.DepartmentMapper;
import com.example.defensemanagement.entity.Department;
import com.example.defensemanagement.service.AuthService;
import com.example.defensemanagement.service.TeacherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

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

    @Autowired
    private DepartmentMapper departmentMapper;

    private User requireDeptAdmin(HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null || !authService.hasPermission(currentUser, "MANAGE_TEACHERS")) {
            return null;
        }
        return currentUser;
    }

    @GetMapping("/teachers")
    public List<Teacher> listDepartmentTeachers(HttpSession session) {
        // 允许所有已登录用户获取教师列表
        User currentUser = (User) session.getAttribute("currentUser");
        Teacher currentTeacher = (Teacher) session.getAttribute("currentTeacher");
        
        if (currentUser == null && currentTeacher == null) {
            return java.util.Collections.emptyList();
        }
        
        // 超级管理员可以查看所有教师
        if (currentUser != null && currentUser.getRole() != null && "SUPER_ADMIN".equals(currentUser.getRole().getName())) {
            return teacherService.findByDepartmentId(null);
        }
        
        // 院系管理员查看本院系教师
        if (currentUser != null && currentUser.getRole() != null && "DEPT_ADMIN".equals(currentUser.getRole().getName())) {
            return teacherService.findByDepartmentId(currentUser.getDepartmentId());
        }
        
        // 教师查看本院系教师
        if (currentTeacher != null) {
            return teacherService.findByDepartmentId(currentTeacher.getDepartmentId());
        }
        
        return java.util.Collections.emptyList();
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

    /**
     * 获取未分配到任何小组的教师列表
     * GET /department/group/unassigned-teachers
     */
    @GetMapping("/unassigned-teachers")
    public List<Map<String, Object>> getUnassignedTeachers(HttpSession session) {
        User u = requireDeptAdmin(session);
        if (u == null) {
            return new ArrayList<>();
        }

        // 获取所有教师（根据权限：超级管理员看全部，院系管理员看本院系）
        List<Teacher> allTeachers;
        if (u.getRole() != null && "SUPER_ADMIN".equals(u.getRole().getName())) {
            allTeachers = teacherService.findByDepartmentId(null);
        } else {
            allTeachers = teacherService.findByDepartmentId(u.getDepartmentId());
        }

        System.out.println("=== 分配教师调试信息 ===");
        System.out.println("用户角色: " + (u.getRole() != null ? u.getRole().getName() : "null"));
        System.out.println("用户院系ID: " + u.getDepartmentId());
        System.out.println("查询到的所有教师数量: " + (allTeachers != null ? allTeachers.size() : 0));
        if (allTeachers != null) {
            for (Teacher t : allTeachers) {
                System.out.println("  教师: " + t.getName() + " (ID: " + t.getId() + ", 院系ID: " + t.getDepartmentId() + ", 状态: " + t.getStatus() + ")");
            }
        }

        // 如果查询结果为空，直接返回
        if (allTeachers == null || allTeachers.isEmpty()) {
            System.out.println("所有教师列表为空");
            return new ArrayList<>();
        }

        // 获取所有已分配教师的ID列表（从 defense_group_teacher 表）
        List<DefenseGroupTeacher> allAssigned = defenseGroupTeacherMapper.findAll();
        System.out.println("已分配教师关联记录数: " + (allAssigned != null ? allAssigned.size() : 0));
        
        final List<Long> assignedTeacherIds = new ArrayList<>();
        if (allAssigned != null && !allAssigned.isEmpty()) {
            for (DefenseGroupTeacher dgt : allAssigned) {
                if (dgt != null && dgt.getTeacherId() != null && !assignedTeacherIds.contains(dgt.getTeacherId())) {
                    assignedTeacherIds.add(dgt.getTeacherId());
                }
            }
        }
        System.out.println("已分配教师ID列表: " + assignedTeacherIds);

        // 过滤出未分配的教师（不在任何小组的教师）
        // 注意：不过滤状态，显示所有未分配的教师（包括禁用的），让管理员决定
        final List<Teacher> unassignedTeachers = new ArrayList<>();
        for (Teacher t : allTeachers) {
            if (t != null && t.getId() != null && !assignedTeacherIds.contains(t.getId())) {
                unassignedTeachers.add(t);
                System.out.println("未分配教师: " + t.getName() + " (ID: " + t.getId() + ", 院系ID: " + t.getDepartmentId() + ", 状态: " + (t.getStatus() != null ? t.getStatus() : "null") + ")");
            } else if (t != null && t.getId() != null) {
                System.out.println("已分配教师（跳过）: " + t.getName() + " (ID: " + t.getId() + ", 在已分配列表中: " + assignedTeacherIds.contains(t.getId()) + ")");
            }
        }
        System.out.println("未分配教师总数: " + unassignedTeachers.size());
        System.out.println("===================");

        // 转换为Map格式返回（包含院系信息）
        List<Map<String, Object>> result = new ArrayList<>();
        for (Teacher t : unassignedTeachers) {
            Map<String, Object> info = new HashMap<>();
            info.put("id", t.getId());
            info.put("teacherNo", t.getTeacherNo());
            info.put("name", t.getName());
            info.put("title", t.getTitle());
            // 获取院系名称
            if (t.getDepartmentId() != null) {
                Department dept = departmentMapper.findById(t.getDepartmentId());
                if (dept != null) {
                    info.put("departmentName", dept.getName());
                } else {
                    info.put("departmentName", null);
                }
            } else {
                info.put("departmentName", null);
            }
            result.add(info);
        }

        return result;
    }

    /**
     * 批量分配教师到小组
     * POST /department/group/assign-teachers
     */
    @PostMapping("/assign-teachers")
    public String assignTeachers(@RequestBody Map<String, Object> request, HttpSession session) {
        User u = requireDeptAdmin(session);
        if (u == null) {
            return "error:权限不足";
        }

        try {
            Long groupId = Long.valueOf(request.get("groupId").toString());
            @SuppressWarnings("unchecked")
            List<Integer> teacherIds = (List<Integer>) request.get("teacherIds");
            
            if (groupId == null || teacherIds == null || teacherIds.isEmpty()) {
                return "error:参数错误";
            }

            DefenseGroup g = defenseGroupMapper.findById(groupId);
            if (g == null) {
                return "error:小组不存在";
            }

            // 批量分配教师到小组
            int successCount = 0;
            int skipCount = 0;
            for (Integer teacherId : teacherIds) {
                Teacher t = teacherService.findById(teacherId.longValue());
                if (t == null) {
                    continue;
                }
                
                // 检查权限：院系管理员只能分配本院系教师
                if (u.getRole() != null && "DEPT_ADMIN".equals(u.getRole().getName())
                        && u.getDepartmentId() != null && !u.getDepartmentId().equals(t.getDepartmentId())) {
                    skipCount++;
                    continue;
                }
                
                // 检查该教师是否已经属于其他小组
                DefenseGroupTeacher existing = defenseGroupTeacherMapper.findByTeacherId(teacherId.longValue());
                if (existing != null && !existing.getGroupId().equals(groupId)) {
                    // 教师已经属于其他小组，跳过
                    skipCount++;
                    continue;
                }
                
                // 如果教师已经在当前小组，跳过（避免重复插入）
                if (existing != null && existing.getGroupId().equals(groupId)) {
                    skipCount++;
                    continue;
                }
                
                // 分配教师到小组
                defenseGroupTeacherMapper.insert(groupId, teacherId.longValue(), 0);
                successCount++;
            }

            if (successCount == teacherIds.size()) {
                return "success";
            } else if (successCount > 0) {
                return "success:部分教师分配成功，成功: " + successCount + "/" + teacherIds.size() + 
                       (skipCount > 0 ? "，跳过: " + skipCount : "");
            } else {
                return "error:所有教师分配失败，可能已属于其他小组或权限不足";
            }
        } catch (Exception e) {
            return "error:" + e.getMessage();
        }
    }

    /**
     * 批量从小组移除教师
     * POST /department/group/remove-teachers
     */
    @PostMapping("/remove-teachers")
    @ResponseBody
    public String removeTeachersFromGroup(@RequestBody Map<String, Object> request, HttpSession session) {
        User u = requireDeptAdmin(session);
        if (u == null) {
            return "error:权限不足";
        }

        try {
            Long groupId = Long.valueOf(request.get("groupId").toString());
            @SuppressWarnings("unchecked")
            List<Integer> teacherIds = (List<Integer>) request.get("teacherIds");
            
            if (groupId == null || teacherIds == null || teacherIds.isEmpty()) {
                return "error:参数错误";
            }

            DefenseGroup g = defenseGroupMapper.findById(groupId);
            if (g == null) {
                return "error:小组不存在";
            }

            // 批量从小组移除教师
            int successCount = 0;
            for (Integer teacherId : teacherIds) {
                try {
                    // 从 defense_group_teacher 表中删除记录
                    defenseGroupTeacherMapper.delete(groupId, teacherId.longValue());
                    successCount++;
                } catch (Exception e) {
                    System.err.println("移除教师失败: " + teacherId + ", 错误: " + e.getMessage());
                }
            }

            if (successCount == 0) {
                return "error:没有成功移除任何教师";
            }

            return "success:已成功从小组移除 " + successCount + " 个教师";
        } catch (Exception e) {
            e.printStackTrace();
            return "error:移除教师失败: " + e.getMessage();
        }
    }
}


