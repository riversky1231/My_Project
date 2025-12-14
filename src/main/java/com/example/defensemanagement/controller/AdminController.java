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

    @GetMapping("/users")
    public String userManagement(Model model, HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null || !authService.hasPermission(currentUser, "CREATE_DEPT_ADMIN")) {
            return "redirect:/";
        }
        
        List<User> deptAdmins = userService.getUsersByRole("DEPT_ADMIN");
        List<Department> departments = userService.getAllDepartments();
        
        model.addAttribute("deptAdmins", deptAdmins);
        model.addAttribute("departments", departments);
        return "admin/users";
    }

    @PostMapping("/user/createDeptAdmin")
    @ResponseBody
    public String createDepartmentAdmin(@RequestParam String username,
                                       @RequestParam String password,
                                       @RequestParam String realName,
                                       @RequestParam String email,
                                       @RequestParam Long departmentId,
                                       HttpSession session) {
        
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null || !authService.hasPermission(currentUser, "CREATE_DEPT_ADMIN")) {
            return "error:权限不足";
        }
        
        try {
            userService.createDepartmentAdmin(username, password, realName, email, departmentId);
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