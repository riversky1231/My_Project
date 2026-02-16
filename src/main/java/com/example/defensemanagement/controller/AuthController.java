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
                       @RequestParam String role,
                       @RequestParam String captcha,
                       HttpSession session, 
                       Model model) {
        
        System.out.println("登录尝试: username=" + username + ", role=" + role);

        // 验证验证码
        String sessionCaptcha = (String) session.getAttribute("captcha");
        if (captcha == null || sessionCaptcha == null || !captcha.toLowerCase().equals(sessionCaptcha)) {
            model.addAttribute("error", "验证码错误");
            return "login";
        }
        
        if ("TEACHER".equals(role)) {
            Teacher teacher = authService.teacherLogin(username, password);
            if (teacher != null) {
                session.setAttribute("currentTeacher", teacher);
                session.setAttribute("userType", "TEACHER");
                return "redirect:/?login=success";
            }
        } else {
            User user = authService.login(username, password);
            if (user != null && user.getRole() != null && role.equals(user.getRole().getName())) {
                session.setAttribute("currentUser", user);
                session.setAttribute("userType", "USER");
                return "redirect:/?login=success";
            }
        }
        
        // 登录失败
        model.addAttribute("error", "用户名、密码或角色选择不匹配");
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