package com.example.defensemanagement.controller;

import com.example.defensemanagement.entity.Student;
import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.entity.Teacher;
import com.example.defensemanagement.entity.DefenseGroup;
import com.example.defensemanagement.service.AuthService;
import com.example.defensemanagement.service.StudentService;
import com.example.defensemanagement.service.ConfigService;
import com.example.defensemanagement.mapper.DefenseGroupMapper;
import com.example.defensemanagement.mapper.TeacherMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpSession;
import java.util.List;

@Controller
@RequestMapping("/department/student")
public class StudentController {

    @Autowired
    private StudentService studentService;

    @Autowired
    private ConfigService configService;
    
    @Autowired
    private DefenseGroupMapper defenseGroupMapper;
    
    @Autowired
    private TeacherMapper teacherMapper;

    // 检查院系管理员或超级管理员权限的辅助方法
    private String checkDeptAdmin(HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "error:权限不足";
        }
        String roleName = currentUser.getRole() != null ? currentUser.getRole().getName() : null;
        // 超级管理员和院系管理员都有权限
        if ("SUPER_ADMIN".equals(roleName) || "DEPT_ADMIN".equals(roleName)) {
            return null; // 权限通过
        }
        return "error:权限不足";
    }
    
    /**
     * 通过 user_id 查找对应的 teacher_id
     */
    private Long findTeacherIdByUserId(Long userId) {
        Teacher teacher = teacherMapper.findByUserId(userId);
        return teacher != null ? teacher.getId() : null;
    }

    /**
     * 获取学生列表
     * - 超级管理员：查看所有学生
     * - 院系管理员：查看本院系的学生
     * - 教师/答辩组长：查看自己指导的学生
     * GET /department/student/list
     */
    @GetMapping("/list")
    @ResponseBody
    public List<Student> getStudentsByDept(HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        Teacher currentTeacher = (Teacher) session.getAttribute("currentTeacher");
        
        // 如果是教师（包括答辩组长），返回其指导的学生
        if (currentTeacher != null) {
            Integer currentYear = configService.getCurrentDefenseYear();
            if (currentYear == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请先设置当前答辩年份");
            }
            // 教师只能查看自己指导的学生
            return studentService.getStudentsByAdvisor(currentTeacher.getId(), currentYear);
        }
        
        // 如果是用户登录
        if (currentUser != null) {
            String roleName = currentUser.getRole() != null ? currentUser.getRole().getName() : null;
            
            // 超级管理员：查看所有学生
            if ("SUPER_ADMIN".equals(roleName)) {
                Integer currentYear = configService.getCurrentDefenseYear();
                if (currentYear == null) {
                    // 如果没有设置年份，返回所有学生
                    return studentService.findAll();
                }
                // 返回当前年份的所有学生
                return studentService.findByYear(currentYear);
            }
            
            // 院系管理员：查看本院系的学生
            if ("DEPT_ADMIN".equals(roleName)) {
                Long departmentId = currentUser.getDepartmentId();
                Integer currentYear = configService.getCurrentDefenseYear();
                
                if (departmentId == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "院系信息未配置，请联系管理员");
                }
                if (currentYear == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请先设置当前答辩年份");
                }
                
                // 院系管理员管理自己系的学生
                List<Student> students = studentService.findByDepartmentAndYear(departmentId, currentYear);
                return students != null ? students : new java.util.ArrayList<>();
            }
            
            // 教师角色：查看自己指导的学生
            if ("TEACHER".equals(roleName)) {
                Integer currentYear = configService.getCurrentDefenseYear();
                if (currentYear == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请先设置当前答辩年份");
                }
                // 通过 user_id 关联查找对应的 teacher 记录
                Long teacherId = findTeacherIdByUserId(currentUser.getId());
                if (teacherId == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "未找到关联的教师信息");
                }
                return studentService.getStudentsByAdvisor(teacherId, currentYear);
            }
        }
        
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "权限不足或未登录");
    }

    /**
     * 获取当前年份（用于前端表单默认值）
     * GET /department/student/currentYear
     */
    @GetMapping("/currentYear")
    @ResponseBody
    public Integer getCurrentYear() {
        return configService.getCurrentDefenseYear();
    }
    
    /**
     * 获取答辩小组列表（用于前端下拉选择）
     * GET /department/student/groups
     */
    @GetMapping("/groups")
    @ResponseBody
    public List<DefenseGroup> getDefenseGroups(HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        Teacher currentTeacher = (Teacher) session.getAttribute("currentTeacher");
        
        // 允许超级管理员、院系管理员和教师访问
        if (currentUser != null) {
            String roleName = currentUser.getRole() != null ? currentUser.getRole().getName() : null;
            if ("SUPER_ADMIN".equals(roleName) || "DEPT_ADMIN".equals(roleName)) {
                // 返回所有答辩小组（按显示顺序排序）
                return defenseGroupMapper.findAllByOrderByDisplayOrderAsc();
            }
        }
        
        // 教师也可以访问（用于查看自己指导的学生所在的小组）
        if (currentTeacher != null) {
            return defenseGroupMapper.findAllByOrderByDisplayOrderAsc();
        }
        
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "权限不足");
    }

    /**
     * 保存或更新学生信息
     * POST /department/student/save
     */
    @PostMapping("/save")
    @ResponseBody
    public String saveStudent(@RequestBody Student student, HttpSession session) {
        String permissionError = checkDeptAdmin(session);
        if (permissionError != null) {
            return permissionError;
        }

        try {
            studentService.saveStudent(student);
            return "success";
        } catch (Exception e) {
            return "error:保存学生信息失败, " + e.getMessage();
        }
    }

    /**
     * 分配指导教师
     * POST /department/student/assign/advisor?studentId=1&teacherId=T001
     */
    @PostMapping("/assign/advisor")
    @ResponseBody
    public String assignAdvisor(@RequestParam Long studentId, @RequestParam Long teacherId, HttpSession session) {
        String permissionError = checkDeptAdmin(session);
        if (permissionError != null) {
            return permissionError;
        }

        try {
            studentService.assignAdvisor(studentId, teacherId);
            return "success";
        } catch (Exception e) {
            return "error:分配指导教师失败, " + e.getMessage();
        }
    }

    /**
     * 分配评阅人
     * POST /department/student/assign/reviewer?studentId=1&teacherId=T002
     */
    @PostMapping("/assign/reviewer")
    @ResponseBody
    public String assignReviewer(@RequestParam Long studentId, @RequestParam Long teacherId, HttpSession session) {
        String permissionError = checkDeptAdmin(session);
        if (permissionError != null) {
            return permissionError;
        }

        try {
            studentService.assignReviewer(studentId, teacherId);
            return "success";
        } catch (Exception e) {
            return "error:分配评阅人失败, " + e.getMessage();
        }
    }

    /**
     * 答辩分组功能：将学生分配到答辩小组
     * POST /department/student/assign/group?studentId=1&groupId=2
     */
    @PostMapping("/assign/group")
    @ResponseBody
    public String assignDefenseGroup(@RequestParam Long studentId, @RequestParam Long groupId, HttpSession session) {
        String permissionError = checkDeptAdmin(session);
        if (permissionError != null) {
            return permissionError;
        }

        try {
            studentService.assignDefenseGroup(studentId, groupId);
            return "success";
        } catch (Exception e) {
            return "error:分配答辩小组失败, " + e.getMessage();
        }
    }

    /**
     * 取消答辩分组：将学生从小组移除（defense_group_id 置空）
     * POST /department/student/unassign/group?studentId=1
     */
    @PostMapping("/unassign/group")
    @ResponseBody
    public String unassignDefenseGroup(@RequestParam Long studentId, HttpSession session) {
        String permissionError = checkDeptAdmin(session);
        if (permissionError != null) {
            return permissionError;
        }
        try {
            studentService.unassignDefenseGroup(studentId);
            return "success";
        } catch (Exception e) {
            return "error:移除失败, " + e.getMessage();
        }
    }

    /**
     * 删除学生
     * DELETE /department/student/{id}
     */
    @DeleteMapping("/{id}")
    @ResponseBody
    public String deleteStudent(@PathVariable Long id, HttpSession session) {
        String permissionError = checkDeptAdmin(session);
        if (permissionError != null) {
            // make it explicit for callers expecting HTTP semantics
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "权限不足");
        }
        try {
            return studentService.deleteStudent(id) ? "success" : "error:删除失败";
        } catch (Exception e) {
            return "error:" + e.getMessage();
        }
    }
}