package com.example.defensemanagement.controller;

import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.entity.Department;
import com.example.defensemanagement.service.UserService;
import com.example.defensemanagement.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private UserService userService;
    
    @Autowired
    private AuthService authService;

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

    @GetMapping("/users/list")
    @ResponseBody
    public List<User> getUserList(HttpSession session) {
        Object userObject = session.getAttribute("currentUser");
        if (userObject == null) {
             userObject = session.getAttribute("currentTeacher");
        }
        if (userObject == null) {
            return null; 
        }
        return userService.getAllUsers();
    }
    
    @GetMapping("/users/search")
    @ResponseBody
    public java.util.Map<String, Object> searchUsers(
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "8") int pageSize,
            HttpSession session) {
        
        Object userObject = session.getAttribute("currentUser");
        if (userObject == null) {
            userObject = session.getAttribute("currentTeacher");
        }
        if (userObject == null) {
            return null;
        }
        
        List<User> users = userService.searchUsers(keyword, page, pageSize);
        int total = userService.countUsers(keyword);
        int totalPages = (int) Math.ceil((double) total / pageSize);
        
        java.util.Map<String, Object> result = new java.util.HashMap<>();
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
        if (currentUser == null) return null;
        return userService.getAllDepartments();
    }

    @GetMapping("/roles/list")
    @ResponseBody
    public List<com.example.defensemanagement.entity.Role> getRoleList(HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) return null;
        return userService.getManagableRoles(currentUser);
    }

    @Autowired
    private com.example.defensemanagement.service.PermissionService permissionService;

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
            // 加载完整的目标用户信息（包括角色和院系信息）
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
        
        // 检查权限：需要能够编辑目标用户
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
        
        // 检查权限：需要能够编辑目标用户
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
}