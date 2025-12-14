package com.example.defensemanagement.controller;

import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.entity.Teacher;
import com.example.defensemanagement.entity.DefenseLeader;
import com.example.defensemanagement.service.TeacherService;
import com.example.defensemanagement.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/department")
public class DepartmentController {

    @Autowired
    private TeacherService teacherService;
    
    @Autowired
    private AuthService authService;

    @GetMapping("/teachers")
    public String teacherManagement(Model model, HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null || !authService.hasPermission(currentUser, "MANAGE_TEACHERS")) {
            return "redirect:/";
        }
        
        List<Teacher> teachers;
        if ("SUPER_ADMIN".equals(currentUser.getRole().getName())) {
            teachers = teacherService.findByDepartmentId(null);
        } else {
            teachers = teacherService.findByDepartmentId(currentUser.getDepartmentId());
        }
        
        model.addAttribute("teachers", teachers);
        return "department/teachers";
    }

    @PostMapping("/teacher/create")
    @ResponseBody
    public String createTeacher(@RequestParam String teacherNo,
                               @RequestParam String name,
                               @RequestParam Long departmentId,
                               @RequestParam String title,
                               @RequestParam String email,
                               @RequestParam String phone,
                               HttpSession session) {
        
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null || !authService.hasPermission(currentUser, "MANAGE_TEACHERS")) {
            return "error:权限不足";
        }
        
        // 院系管理员只能在自己的院系创建教师
        if ("DEPT_ADMIN".equals(currentUser.getRole().getName()) && 
            !departmentId.equals(currentUser.getDepartmentId())) {
            return "error:只能在本院系创建教师";
        }
        
        try {
            teacherService.createTeacher(teacherNo, name, departmentId, title, email, phone);
            return "success";
        } catch (Exception e) {
            return "error:" + e.getMessage();
        }
    }

    @GetMapping("/defenseLeaders")
    public String defenseLeaderManagement(Model model, HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null || !authService.hasPermission(currentUser, "SET_DEFENSE_LEADER")) {
            return "redirect:/";
        }
        
        int currentYear = LocalDate.now().getYear();
        List<DefenseLeader> leaders;
        
        if ("SUPER_ADMIN".equals(currentUser.getRole().getName())) {
            leaders = teacherService.getDefenseLeadersByYear(currentYear);
        } else {
            leaders = teacherService.getDefenseLeadersByDepartmentAndYear(currentUser.getDepartmentId(), currentYear);
        }
        
        model.addAttribute("leaders", leaders);
        model.addAttribute("currentYear", currentYear);
        return "department/defenseLeaders";
    }

    @PostMapping("/defenseLeader/set")
    @ResponseBody
    public String setDefenseLeader(@RequestParam Long teacherId,
                                  @RequestParam Integer year,
                                  HttpSession session) {
        
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null || !authService.hasPermission(currentUser, "SET_DEFENSE_LEADER")) {
            return "error:权限不足";
        }
        
        Teacher teacher = teacherService.findById(teacherId);
        if (teacher == null) {
            return "error:教师不存在";
        }
        
        if ("DEPT_ADMIN".equals(currentUser.getRole().getName()) &&
            !teacher.getDepartmentId().equals(currentUser.getDepartmentId())) {
            return "error:只能设置本院系的答辩组长";
        }
        
        if (teacherService.setDefenseLeader(teacherId, year, teacher.getDepartmentId())) {
            return "success";
        } else {
            return "error:设置失败";
        }
    }

    @PostMapping("/defenseLeader/remove")
    @ResponseBody
    public String removeDefenseLeader(@RequestParam Long teacherId,
                                     @RequestParam Integer year,
                                     HttpSession session) {
        
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null || !authService.hasPermission(currentUser, "SET_DEFENSE_LEADER")) {
            return "error:权限不足";
        }
        
        if (teacherService.removeDefenseLeader(teacherId, year)) {
            return "success";
        } else {
            return "error:取消失败";
        }
    }
}