package com.example.defensemanagement.controller;

import com.example.defensemanagement.entity.Student;
import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.entity.Teacher;
import com.example.defensemanagement.entity.DefenseGroup;
import com.example.defensemanagement.entity.StudentFinalScore;
import com.example.defensemanagement.service.AuthService;
import com.example.defensemanagement.service.StudentService;
import com.example.defensemanagement.service.ConfigService;
import com.example.defensemanagement.service.ScoreService;
import com.example.defensemanagement.mapper.DefenseGroupMapper;
import com.example.defensemanagement.mapper.TeacherMapper;
import com.example.defensemanagement.mapper.StudentFinalScoreMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.stream.Collectors;

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
    
    @Autowired
    private StudentFinalScoreMapper studentFinalScoreMapper;
    
    @Autowired
    private ScoreService scoreService;

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
    
    // ======================= 教师专用接口 =======================
    
    /**
     * 从 Session 中获取当前教师
     */
    private Teacher getTeacherFromSession(HttpSession session) {
        // 先尝试从 session 获取教师
        Teacher teacher = (Teacher) session.getAttribute("currentTeacher");
        if (teacher != null) {
            return teacher;
        }
        
        // 如果是 User 登录，检查是否是教师角色
        User user = (User) session.getAttribute("currentUser");
        if (user != null && user.getRole() != null) {
            String roleName = user.getRole().getName();
            if ("TEACHER".equals(roleName) || "DEFENSE_LEADER".equals(roleName)) {
                // 通过 user_id 查找关联的教师记录
                teacher = teacherMapper.findByUserId(user.getId());
                if (teacher != null) {
                    return teacher;
                }
                // 如果通过 userId 找不到，尝试通过用户名（假设用户名是教师工号）
                teacher = teacherMapper.findByTeacherNo(user.getUsername());
                return teacher;
            }
        }
        return null;
    }
    
    /**
     * 获取教师指导的学生列表（含成绩信息）
     * GET /department/student/teacher/advised
     */
    @GetMapping("/teacher/advised")
    @ResponseBody
    public Map<String, Object> getAdvisedStudentsWithScores(HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        
        Teacher teacher = getTeacherFromSession(session);
        if (teacher == null) {
            result.put("error", "请先登录教师账号");
            return result;
        }
        
        Integer currentYear = configService.getCurrentDefenseYear();
        if (currentYear == null) {
            result.put("error", "请先设置当前答辩年份");
            return result;
        }
        
        // 获取指导的学生列表
        List<Student> students = studentService.getStudentsByAdvisor(teacher.getId(), currentYear);
        
        // 获取学生成绩信息
        List<Map<String, Object>> studentList = new ArrayList<>();
        if (students != null) {
            List<Long> studentIds = students.stream().map(Student::getId).collect(Collectors.toList());
            List<StudentFinalScore> scores = studentIds.isEmpty() ? new ArrayList<>() : 
                studentFinalScoreMapper.findByStudentIdsAndYear(studentIds, currentYear);
            Map<Long, StudentFinalScore> scoreMap = scores.stream()
                .collect(Collectors.toMap(StudentFinalScore::getStudentId, s -> s));
            
            for (Student s : students) {
                Map<String, Object> info = new HashMap<>();
                info.put("id", s.getId());
                info.put("studentNo", s.getStudentNo());
                info.put("name", s.getName());
                info.put("classInfo", s.getClassInfo());
                info.put("defenseType", s.getDefenseType());
                info.put("title", s.getTitle());
                info.put("defenseYear", s.getDefenseYear());
                info.put("advisorTeacherId", s.getAdvisorTeacherId());
                info.put("reviewerTeacherId", s.getReviewerTeacherId());
                
                // 设置评阅教师信息
                if (s.getReviewer() != null) {
                    info.put("reviewerName", s.getReviewer().getName());
                    info.put("reviewerTeacherNo", s.getReviewer().getTeacherNo());
                } else {
                    info.put("reviewerName", null);
                    info.put("reviewerTeacherNo", null);
                }
                
                // 设置答辩小组信息
                info.put("defenseGroupId", s.getDefenseGroupId());
                if (s.getDefenseGroup() != null) {
                    info.put("defenseGroupName", s.getDefenseGroup().getName());
                } else {
                    info.put("defenseGroupName", null);
                }
                
                // 设置成绩信息
                StudentFinalScore score = scoreMap.get(s.getId());
                if (score != null) {
                    info.put("advisorScore", score.getAdvisorScore());
                    info.put("reviewerScore", score.getReviewerScore());
                    info.put("finalDefenseScore", score.getFinalDefenseScore());
                    info.put("totalGrade", score.getTotalGrade());
                } else {
                    info.put("advisorScore", null);
                    info.put("reviewerScore", null);
                    info.put("finalDefenseScore", null);
                    info.put("totalGrade", null);
                }
                
                studentList.add(info);
            }
        }
        
        result.put("students", studentList);
        result.put("teacherId", teacher.getId());
        result.put("teacherName", teacher.getName());
        result.put("year", currentYear);
        return result;
    }
    
    /**
     * 教师设置指导教师评定成绩
     * POST /department/student/teacher/setAdvisorScore
     */
    @PostMapping("/teacher/setAdvisorScore")
    @ResponseBody
    public String setAdvisorScoreByTeacher(@RequestParam Long studentId, @RequestParam Integer score, HttpSession session) {
        Teacher teacher = getTeacherFromSession(session);
        if (teacher == null) {
            return "error:请先登录教师账号";
        }
        
        Integer currentYear = configService.getCurrentDefenseYear();
        if (currentYear == null) {
            return "error:请先设置当前答辩年份";
        }
        
        // 验证该学生是否是当前教师指导的
        Student student = studentService.findById(studentId);
        if (student == null) {
            return "error:学生不存在";
        }
        if (student.getAdvisorTeacherId() == null || !student.getAdvisorTeacherId().equals(teacher.getId())) {
            return "error:您不是该学生的指导教师";
        }
        
        try {
            scoreService.setAdvisorScore(studentId, currentYear, score);
            return "success";
        } catch (Exception e) {
            return "error:" + e.getMessage();
        }
    }
    
    /**
     * 教师为学生指定评阅人
     * POST /department/student/teacher/assignReviewer
     */
    @PostMapping("/teacher/assignReviewer")
    @ResponseBody
    public String assignReviewerByTeacher(@RequestParam Long studentId, @RequestParam Long reviewerId, HttpSession session) {
        Teacher teacher = getTeacherFromSession(session);
        if (teacher == null) {
            return "error:请先登录教师账号";
        }
        
        // 验证该学生是否是当前教师指导的
        Student student = studentService.findById(studentId);
        if (student == null) {
            return "error:学生不存在";
        }
        if (student.getAdvisorTeacherId() == null || !student.getAdvisorTeacherId().equals(teacher.getId())) {
            return "error:您不是该学生的指导教师";
        }
        
        try {
            studentService.assignReviewer(studentId, reviewerId);
            return "success";
        } catch (Exception e) {
            return "error:" + e.getMessage();
        }
    }
    
    /**
     * 获取当前教师作为评阅人的学生列表（含成绩信息）
     * GET /department/student/teacher/reviewed
     */
    @GetMapping("/teacher/reviewed")
    @ResponseBody
    public Map<String, Object> getReviewedStudentsWithScores(HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        
        Teacher teacher = getTeacherFromSession(session);
        if (teacher == null) {
            result.put("error", "请先登录教师账号");
            return result;
        }
        
        Integer currentYear = configService.getCurrentDefenseYear();
        if (currentYear == null) {
            result.put("error", "请先设置当前答辩年份");
            return result;
        }
        
        // 获取作为评阅人的学生列表
        List<Student> students = studentService.getStudentsByReviewer(teacher.getId(), currentYear);
        
        // 获取学生成绩信息
        List<Map<String, Object>> studentList = new ArrayList<>();
        if (students != null) {
            List<Long> studentIds = students.stream().map(Student::getId).collect(Collectors.toList());
            List<StudentFinalScore> scores = studentIds.isEmpty() ? new ArrayList<>() : 
                studentFinalScoreMapper.findByStudentIdsAndYear(studentIds, currentYear);
            Map<Long, StudentFinalScore> scoreMap = scores.stream()
                .collect(Collectors.toMap(StudentFinalScore::getStudentId, s -> s));
            
            for (Student s : students) {
                Map<String, Object> info = new HashMap<>();
                info.put("id", s.getId());
                info.put("studentNo", s.getStudentNo());
                info.put("name", s.getName());
                info.put("classInfo", s.getClassInfo());
                info.put("defenseType", s.getDefenseType());
                info.put("title", s.getTitle());
                info.put("defenseYear", s.getDefenseYear());
                info.put("advisorTeacherId", s.getAdvisorTeacherId());
                info.put("reviewerTeacherId", s.getReviewerTeacherId());
                
                // 设置指导教师信息
                if (s.getAdvisor() != null) {
                    info.put("advisorName", s.getAdvisor().getName());
                    info.put("advisorTeacherNo", s.getAdvisor().getTeacherNo());
                } else {
                    info.put("advisorName", null);
                    info.put("advisorTeacherNo", null);
                }
                
                // 设置答辩小组信息
                info.put("defenseGroupId", s.getDefenseGroupId());
                if (s.getDefenseGroup() != null) {
                    info.put("defenseGroupName", s.getDefenseGroup().getName());
                } else {
                    info.put("defenseGroupName", null);
                }
                
                // 设置成绩信息
                StudentFinalScore score = scoreMap.get(s.getId());
                if (score != null) {
                    info.put("advisorScore", score.getAdvisorScore());
                    info.put("reviewerScore", score.getReviewerScore());
                    info.put("finalDefenseScore", score.getFinalDefenseScore());
                    info.put("totalGrade", score.getTotalGrade());
                } else {
                    info.put("advisorScore", null);
                    info.put("reviewerScore", null);
                    info.put("finalDefenseScore", null);
                    info.put("totalGrade", null);
                }
                
                studentList.add(info);
            }
        }
        
        result.put("students", studentList);
        result.put("teacherId", teacher.getId());
        result.put("teacherName", teacher.getName());
        result.put("year", currentYear);
        return result;
    }
    
    /**
     * 教师设置评阅人评定成绩
     * POST /department/student/teacher/setReviewerScore
     */
    @PostMapping("/teacher/setReviewerScore")
    @ResponseBody
    public String setReviewerScoreByTeacher(@RequestParam Long studentId, @RequestParam Integer score, HttpSession session) {
        Teacher teacher = getTeacherFromSession(session);
        if (teacher == null) {
            return "error:请先登录教师账号";
        }
        
        Integer currentYear = configService.getCurrentDefenseYear();
        if (currentYear == null) {
            return "error:请先设置当前答辩年份";
        }
        
        // 验证该学生是否是当前教师评阅的
        Student student = studentService.findById(studentId);
        if (student == null) {
            return "error:学生不存在";
        }
        if (student.getReviewerTeacherId() == null || !student.getReviewerTeacherId().equals(teacher.getId())) {
            return "error:您不是该学生的评阅人";
        }
        
        try {
            scoreService.setReviewerScore(studentId, currentYear, score);
            return "success";
        } catch (Exception e) {
            return "error:" + e.getMessage();
        }
    }
    
    /**
     * 获取可选的评阅教师列表（所有教师，排除自己）
     * GET /department/student/teacher/reviewerCandidates
     */
    @GetMapping("/teacher/reviewerCandidates")
    @ResponseBody
    public List<Map<String, Object>> getReviewerCandidates(HttpSession session) {
        Teacher currentTeacher = getTeacherFromSession(session);
        List<Map<String, Object>> candidates = new ArrayList<>();
        
        User currentUser = (User) session.getAttribute("currentUser");
        Long departmentId = null;
        if (currentTeacher != null) {
            departmentId = currentTeacher.getDepartmentId();
        } else if (currentUser != null) {
            departmentId = currentUser.getDepartmentId();
        }
        
        // 获取同院系的教师列表
        List<Teacher> teachers = teacherMapper.findByDepartmentId(departmentId);
        if (teachers != null) {
            for (Teacher t : teachers) {
                // 排除自己
                if (currentTeacher != null && t.getId().equals(currentTeacher.getId())) {
                    continue;
                }
                Map<String, Object> info = new HashMap<>();
                info.put("id", t.getId());
                info.put("teacherNo", t.getTeacherNo());
                info.put("name", t.getName());
                info.put("title", t.getTitle());
                candidates.add(info);
            }
        }
        
        return candidates;
    }
}