package com.example.defensemanagement.controller;

import com.example.defensemanagement.entity.Teacher;
import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

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

        System.out.println("Login attempt: username=" + username + ", role=" + role);

        // Validate captcha
        String sessionCaptcha = (String) session.getAttribute("captcha");
        if (captcha == null || sessionCaptcha == null || !captcha.toLowerCase().equals(sessionCaptcha)) {
            model.addAttribute("error", "Invalid captcha.");
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

        // Login failed
        model.addAttribute("error", "Invalid username, password, or role.");
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

        System.out.println("changePassword called. oldPassword="
                + (oldPassword != null ? "***" : "null")
                + ", newPassword=" + (newPassword != null ? "***" : "null"));

        String userType = (String) session.getAttribute("userType");
        System.out.println("userType: " + userType);

        if ("USER".equals(userType)) {
            User currentUser = (User) session.getAttribute("currentUser");
            System.out.println("currentUser: " + (currentUser != null ? currentUser.getUsername() : "null"));
            if (currentUser != null) {
                boolean result = authService.changeUserPassword(currentUser.getId(), oldPassword, newPassword);
                System.out.println("changeUserPassword result: " + result);
                if (result) {
                    return "success";
                } else {
                    return "error:old password incorrect or user not found";
                }
            } else {
                return "error:current user not found";
            }
        } else if ("TEACHER".equals(userType)) {
            Teacher currentTeacher = (Teacher) session.getAttribute("currentTeacher");
            System.out.println("currentTeacher: " + (currentTeacher != null ? currentTeacher.getTeacherNo() : "null"));
            if (currentTeacher != null) {
                boolean result = authService.changeTeacherPassword(currentTeacher.getId(), oldPassword, newPassword);
                System.out.println("changeTeacherPassword result: " + result);
                if (result) {
                    return "success";
                } else {
                    return "error:old password incorrect or teacher not found";
                }
            } else {
                return "error:current teacher not found";
            }
        }

        return "error:unknown user type";
    }
}
