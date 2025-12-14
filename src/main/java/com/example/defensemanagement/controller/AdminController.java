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
        return userService.getAllRoles();
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

        // Fetch the full target user object to check its role and department
        User targetUser = userService.findById(user.getId() != null ? user.getId() : 0L);
        if (user.getId() == null) { // For new user creation, only super admin is allowed
             if (!(currentUserObj instanceof User) || !"SUPER_ADMIN".equals(((User)currentUserObj).getRole().getName())) {
                 return "error:只有超级管理员才能创建用户";
             }
        } else { // For updates, check permission
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
        
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null || !authService.hasPermission(currentUser, "CREATE_DEPT_ADMIN")) {
            return "error:权限不足";
        }
        
        if (userService.updateUserStatus(id, status)) {
            return "success";
        } else {
            return "error:更新失败";
        }
    }
}