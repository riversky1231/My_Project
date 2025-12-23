package com.example.defensemanagement.controller;

import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.entity.Teacher;
import com.example.defensemanagement.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;

@Controller
public class AuthController {

    @Autowired
    private AuthService authService;

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username, 
                       @RequestParam String password, 
                       HttpSession session, 
                       Model model) {
        
        System.out.println("登录尝试: username=" + username + ", password=" + password);
        
        // 首先尝试用户登录
        User user = authService.login(username, password);
        System.out.println("用户登录结果: " + user);
        
        if (user != null) {
            session.setAttribute("currentUser", user);
            session.setAttribute("userType", "USER");
            System.out.println("用户登录成功，重定向到首页");
            return "redirect:/?login=success";
        }
        
        // 如果用户登录失败，尝试教师登录
        Teacher teacher = authService.teacherLogin(username, password);
        System.out.println("教师登录结果: " + teacher);
        
        if (teacher != null) {
            session.setAttribute("currentTeacher", teacher);
            session.setAttribute("userType", "TEACHER");
            System.out.println("教师登录成功，重定向到首页");
            return "redirect:/?login=success";
        }
        
        // 登录失败
        System.out.println("登录失败");
        model.addAttribute("error", "用户名或密码错误");
        return "login";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login?logout=true";
    }

    @PostMapping("/changePassword")
    @ResponseBody
    public String changePassword(@RequestParam String oldPassword,
                                @RequestParam String newPassword,
                                HttpSession session) {
        
        System.out.println("changePassword 被调用: oldPassword=" + (oldPassword != null ? "***" : "null") + ", newPassword=" + (newPassword != null ? "***" : "null"));
        
        String userType = (String) session.getAttribute("userType");
        System.out.println("userType: " + userType);
        
        if ("USER".equals(userType)) {
            User currentUser = (User) session.getAttribute("currentUser");
            System.out.println("currentUser: " + (currentUser != null ? currentUser.getUsername() : "null"));
            if (currentUser != null) {
                boolean result = authService.changeUserPassword(currentUser.getId(), oldPassword, newPassword);
                System.out.println("changeUserPassword 结果: " + result);
                if (result) {
                    return "success";
                } else {
                    return "error:旧密码错误或用户不存在";
                }
            } else {
                return "error:未找到当前用户";
            }
        } else if ("TEACHER".equals(userType)) {
            Teacher currentTeacher = (Teacher) session.getAttribute("currentTeacher");
            System.out.println("currentTeacher: " + (currentTeacher != null ? currentTeacher.getTeacherNo() : "null"));
            if (currentTeacher != null) {
                boolean result = authService.changeTeacherPassword(currentTeacher.getId(), oldPassword, newPassword);
                System.out.println("changeTeacherPassword 结果: " + result);
                if (result) {
                    return "success";
                } else {
                    return "error:旧密码错误或教师不存在";
                }
            } else {
                return "error:未找到当前教师";
            }
        }
        
        return "error:用户类型未知";
    }
}