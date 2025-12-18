package com.example.defensemanagement.controller;

import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.entity.Department;
import com.example.defensemanagement.service.UserService;
import com.example.defensemanagement.service.AuthService;
import com.example.defensemanagement.service.PermissionService;
import com.example.defensemanagement.service.TeacherService;
import com.example.defensemanagement.mapper.TeacherMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private UserService userService;

    @Autowired
    private AuthService authService;

    @Autowired
    private PermissionService permissionService;
    
    @Autowired
    private TeacherService teacherService;
    
    @Autowired
    private TeacherMapper teacherMapper;

    @GetMapping("/departments")
    public String departmentManagement(Model model, HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null || !authService.hasPermission(currentUser, "CREATE_DEPARTMENT")) {
            return "redirect:/";
        }

        List<Department> departments = userService.getAllDepartments();
        model.addAttribute("departments", departments);
        return "admin/departments";
    }

    @PostMapping("/department/create")
    @ResponseBody
    public String createDepartment(@RequestParam String name,
                                   @RequestParam String code,
                                   @RequestParam String description,
                                   HttpSession session) {

        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null || !authService.hasPermission(currentUser, "CREATE_DEPARTMENT")) {
            return "error:权限不足";
        }

        try {
            userService.createDepartment(name, code, description);
            return "success";
        } catch (Exception e) {
            return "error:" + e.getMessage();
        }
    }
    
    @PostMapping("/department/update")
    @ResponseBody
    public String updateDepartment(@RequestBody Department department, HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null || !authService.hasPermission(currentUser, "CREATE_DEPARTMENT")) {
            return "error:权限不足";
        }
        
        try {
            if (userService.updateDepartment(department)) {
                return "success";
            } else {
                return "error:更新失败";
            }
        } catch (Exception e) {
            return "error:" + e.getMessage();
        }
    }
    
    @DeleteMapping("/department/{id}")
    @ResponseBody
    public String deleteDepartment(@PathVariable Long id, HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null || !authService.hasPermission(currentUser, "CREATE_DEPARTMENT")) {
            return "error:权限不足";
        }
        
        try {
            // 检查是否有用户或学生关联到此院系
            List<User> users = userService.getAllUsers(id);
            if (users != null && !users.isEmpty()) {
                return "error:该院系下还有用户，无法删除";
            }
            
            // 删除院系
            if (userService.deleteDepartment(id)) {
                return "success";
            } else {
                return "error:删除失败";
            }
        } catch (Exception e) {
            return "error:" + e.getMessage();
        }
    }

    // 修改：传入 departmentId
    @GetMapping("/users/list")
    @ResponseBody
    public List<User> getUserList(HttpSession session) {
        Object userObject = session.getAttribute("currentUser");
        if (userObject == null) {
            userObject = session.getAttribute("currentTeacher");
        }
        if (userObject == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录");
        }

        Long departmentId = getDepartmentIdIfDeptAdmin(userObject);
        return userService.getAllUsers(departmentId);
    }

    // 修改：传入 departmentId
    @GetMapping("/users/search")
    @ResponseBody
    public Map<String, Object> searchUsers(
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "8") int pageSize,
            HttpSession session) {

        Object userObject = session.getAttribute("currentUser");
        if (userObject == null) {
            userObject = session.getAttribute("currentTeacher");
        }
        if (userObject == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录");
        }

        Long departmentId = getDepartmentIdIfDeptAdmin(userObject);

        List<User> users = userService.searchUsers(keyword, page, pageSize, departmentId);
        int total = userService.countUsers(keyword, departmentId);
        int totalPages = (int) Math.ceil((double) total / pageSize);

        Map<String, Object> result = new HashMap<>();
        result.put("users", users);
        result.put("total", total);
        result.put("currentPage", page);
        result.put("pageSize", pageSize);
        result.put("totalPages", totalPages);

        return result;
    }

    @GetMapping("/departments/list")
    @ResponseBody
    public List<Department> getDepartmentList(HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录");
        }
        return userService.getAllDepartments();
    }

    @GetMapping("/roles/list")
    @ResponseBody
    public List<com.example.defensemanagement.entity.Role> getRoleList(HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录");
        }
        return userService.getManagableRoles(currentUser);
    }

    @PostMapping("/users/save")
    @ResponseBody
    public String saveUser(@RequestBody User user, HttpSession session) {
        Object currentUserObj = session.getAttribute("currentUser");
        if (currentUserObj == null) {
            currentUserObj = session.getAttribute("currentTeacher");
        }
        if (currentUserObj == null) {
            return "error:未登录";
        }

        // Check permission for user creation/update
        if (user.getId() == null) { // For new user creation
            if (!permissionService.canCreateUser(currentUserObj, user)) {
                return "error:权限不足，无法创建该角色的用户";
            }
        } else { // For updates, check permission
            User targetUser = userService.findById(user.getId());
            if (targetUser == null) {
                return "error:目标用户不存在";
            }
            if (!permissionService.canEditUser(currentUserObj, targetUser)) {
                return "error:权限不足";
            }
        }

        try {
            userService.saveUser(user);
            return "success";
        } catch (Exception e) {
            return "error:" + e.getMessage();
        }
    }

    @PostMapping("/user/{id}/status")
    @ResponseBody
    public String updateUserStatus(@PathVariable Long id,
                                   @RequestParam Integer status,
                                   HttpSession session) {

        Object currentUserObj = session.getAttribute("currentUser");
        if (currentUserObj == null) {
            currentUserObj = session.getAttribute("currentTeacher");
        }
        if (currentUserObj == null) {
            return "error:未登录";
        }

        User targetUser = userService.findById(id);
        if (targetUser == null) {
            return "error:目标用户不存在";
        }

        if (!permissionService.canEditUser(currentUserObj, targetUser)) {
            return "error:权限不足";
        }

        if (userService.updateUserStatus(id, status)) {
            return "success";
        } else {
            return "error:更新失败";
        }
    }

    @DeleteMapping("/user/{id}")
    @ResponseBody
    public String deleteUser(@PathVariable Long id, HttpSession session) {
        Object currentUserObj = session.getAttribute("currentUser");
        if (currentUserObj == null) {
            currentUserObj = session.getAttribute("currentTeacher");
        }
        if (currentUserObj == null) {
            return "error:未登录";
        }

        User targetUser = userService.findById(id);
        if (targetUser == null) {
            return "error:目标用户不存在";
        }

        if (!permissionService.canEditUser(currentUserObj, targetUser)) {
            return "error:权限不足";
        }

        // 防止删除自己
        if (currentUserObj instanceof User) {
            User currentUser = (User) currentUserObj;
            if (currentUser.getId().equals(id)) {
                return "error:不能删除自己";
            }
        }

        try {
            if (userService.deleteUser(id)) {
                return "success";
            } else {
                return "error:删除失败";
            }
        } catch (Exception e) {
            return "error:" + e.getMessage();
        }
    }

    /**
     * 检查用户是否是答辩组长
     * GET /admin/user/{userId}/isDefenseLeader
     */
    @GetMapping("/user/{userId}/isDefenseLeader")
    @ResponseBody
    public boolean isDefenseLeader(@PathVariable Long userId, HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return false;
        }
        
        // 查找对应的教师ID
        User targetUser = userService.findById(userId);
        if (targetUser == null || targetUser.getRole() == null || !"TEACHER".equals(targetUser.getRole().getName())) {
            return false;
        }
        
        // 查找教师记录
        com.example.defensemanagement.entity.Teacher teacher = teacherMapper.findByUserId(userId);
        if (teacher == null) {
            return false;
        }
        
        // 检查是否是答辩组长（使用当前年份）
        java.time.LocalDate now = java.time.LocalDate.now();
        return authService.isDefenseLeader(teacher.getId(), now.getYear());
    }
    
    // 辅助方法：如果是院系管理员，返回其院系ID；否则返回null
    private Long getDepartmentIdIfDeptAdmin(Object userObj) {
        if (userObj instanceof User) {
            User user = (User) userObj;
            if (user.getRole() != null && "DEPT_ADMIN".equals(user.getRole().getName())) {
                return user.getDepartmentId();
            }
        }
        return null;
    }
}