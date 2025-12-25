package com.example.defensemanagement.controller;

import com.example.defensemanagement.entity.Student;
import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.entity.Teacher;
import com.example.defensemanagement.entity.DefenseGroup;
import com.example.defensemanagement.entity.StudentFinalScore;
import com.example.defensemanagement.entity.TeacherScoreRecord;
import com.example.defensemanagement.service.StudentService;
import com.example.defensemanagement.service.ConfigService;
import com.example.defensemanagement.service.ScoreService;
import com.example.defensemanagement.mapper.DefenseGroupMapper;
import com.example.defensemanagement.mapper.TeacherMapper;
import com.example.defensemanagement.mapper.StudentFinalScoreMapper;
import com.example.defensemanagement.mapper.TeacherScoreRecordMapper;
import com.example.defensemanagement.mapper.DefenseGroupTeacherMapper;
import com.example.defensemanagement.mapper.StudentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpSession;
import org.springframework.web.multipart.MultipartFile;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import java.io.InputStream;
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
    
    @Autowired
    private TeacherScoreRecordMapper teacherScoreRecordMapper;
    
    @Autowired
    private DefenseGroupTeacherMapper defenseGroupTeacherMapper;
    
    @Autowired
    private StudentMapper studentMapper;

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
     * Excel导入学生信息
     * POST /department/student/import/excel
     * Excel格式：第一行为表头（必须包含：学号、姓名），从第二行开始为数据
     * 类型和题目为可选列，如果存在则读取，不存在则为空
     */
    @PostMapping("/import/excel")
    @ResponseBody
    public String importStudentsFromExcel(@RequestParam("file") MultipartFile file, HttpSession session) {
        String permissionError = checkDeptAdmin(session);
        if (permissionError != null) {
            return permissionError;
        }

        if (file == null || file.isEmpty()) {
            return "error:请选择Excel文件";
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || (!fileName.endsWith(".xlsx") && !fileName.endsWith(".xls"))) {
            return "error:文件格式不正确，请上传.xlsx或.xls格式的Excel文件";
        }

        int successCount = 0;
        int failCount = 0;
        StringBuilder errorMessages = new StringBuilder();

        try {
            InputStream inputStream = file.getInputStream();
            Workbook workbook;
            
            // 根据文件扩展名创建不同的Workbook
            if (fileName.endsWith(".xlsx")) {
                workbook = new XSSFWorkbook(inputStream);
            } else {
                workbook = new HSSFWorkbook(inputStream);
            }

            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null || sheet.getPhysicalNumberOfRows() < 2) {
                workbook.close();
                return "error:Excel文件至少需要包含表头和数据行（至少2行）";
            }

            // 获取当前用户信息
            User currentUser = (User) session.getAttribute("currentUser");
            Long departmentId = null;
            if (currentUser != null && currentUser.getDepartmentId() != null) {
                departmentId = currentUser.getDepartmentId();
            }

            // 获取当前答辩年份
            Integer currentYear = configService.getCurrentDefenseYear();
            if (currentYear == null) {
                currentYear = java.time.Year.now().getValue(); // 默认使用当前年份
            }

            // 读取第一行，尝试识别表头或数据
            Row firstRow = sheet.getRow(0);
            if (firstRow == null) {
                workbook.close();
                return "error:Excel文件第一行不能为空";
            }

            int studentNoCol = -1;
            int nameCol = -1;
            int typeCol = -1;
            int titleCol = -1;
            int startRowIndex = 0; // 数据开始的行索引

            // 首先尝试识别表头
            boolean hasHeader = false;
            for (int i = 0; i < firstRow.getPhysicalNumberOfCells(); i++) {
                Cell cell = firstRow.getCell(i);
                if (cell != null) {
                    String cellValue = getCellValueAsString(cell).trim();
                    if (cellValue.contains("学号") || cellValue.equalsIgnoreCase("studentNo") || cellValue.equalsIgnoreCase("student_no") || cellValue.equalsIgnoreCase("学号")) {
                        studentNoCol = i;
                        hasHeader = true;
                    } else if (cellValue.contains("姓名") || cellValue.equalsIgnoreCase("name") || cellValue.equalsIgnoreCase("姓名")) {
                        nameCol = i;
                        hasHeader = true;
                    } else if (cellValue.contains("类型") || cellValue.equalsIgnoreCase("type") || cellValue.equalsIgnoreCase("defenseType")) {
                        typeCol = i;
                        hasHeader = true;
                    } else if (cellValue.contains("题目") || cellValue.equalsIgnoreCase("title")) {
                        titleCol = i;
                        hasHeader = true;
                    }
                }
            }

            // 如果找到了表头，数据从第二行开始
            if (hasHeader && (studentNoCol != -1 || nameCol != -1)) {
                startRowIndex = 1;
            } else {
                // 如果没有找到表头，假设第一列是学号，第二列是姓名
                studentNoCol = 0;
                nameCol = 1;
                startRowIndex = 0; // 从第一行开始读取数据
            }

            // 验证必需的列是否存在（只需要学号和姓名）
            if (studentNoCol == -1 || nameCol == -1) {
                workbook.close();
                return "error:Excel文件必须包含学号和姓名列（第一列学号，第二列姓名，或使用表头标识）";
            }

            // 从指定行开始读取数据
            for (int rowIndex = startRowIndex; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }

                try {
                    // 读取各列数据（只读取学号和姓名，类型和题目为可选）
                    String studentNo = getCellValueAsString(row.getCell(studentNoCol)).trim();
                    String name = getCellValueAsString(row.getCell(nameCol)).trim();
                    String defenseType = null;
                    String title = null;
                    
                    // 如果存在类型列，读取类型
                    if (typeCol != -1) {
                        defenseType = getCellValueAsString(row.getCell(typeCol)).trim();
                        if (defenseType != null && !defenseType.isEmpty()) {
                            // 验证类型值（PAPER或DESIGN）
                            if ("论文".equals(defenseType)) {
                                defenseType = "PAPER";
                            } else if ("设计".equals(defenseType)) {
                                defenseType = "DESIGN";
                            } else {
                                defenseType = defenseType.toUpperCase();
                            }
                            // 验证类型值是否有效
                            if (!"PAPER".equals(defenseType) && !"DESIGN".equals(defenseType)) {
                                defenseType = null; // 无效类型，置为空
                            }
                        } else {
                            defenseType = null;
                        }
                    }
                    
                    // 如果存在题目列，读取题目
                    if (titleCol != -1) {
                        title = getCellValueAsString(row.getCell(titleCol)).trim();
                        if (title != null && title.isEmpty()) {
                            title = null;
                        }
                    }

                    // 验证必填字段（只需要学号和姓名）
                    if (studentNo == null || studentNo.isEmpty()) {
                        failCount++;
                        errorMessages.append("第").append(rowIndex + 1).append("行：学号不能为空；");
                        continue;
                    }
                    if (name == null || name.isEmpty()) {
                        failCount++;
                        errorMessages.append("第").append(rowIndex + 1).append("行：姓名不能为空；");
                        continue;
                    }

                    // 检查学号是否已存在（同一年份）
                    Student existingStudent = studentMapper.findByStudentNoAndYear(studentNo, currentYear);
                    if (existingStudent != null) {
                        failCount++;
                        errorMessages.append("第").append(rowIndex + 1).append("行：学号").append(studentNo).append("在").append(currentYear).append("年已存在；");
                        continue;
                    }

                    // 创建学生对象
                    Student student = new Student();
                    student.setStudentNo(studentNo);
                    student.setName(name);
                    student.setDefenseType(defenseType); // 可能为null
                    student.setTitle(title); // 可能为null
                    student.setDefenseYear(currentYear);
                    student.setDepartmentId(departmentId);
                    // 其他字段保持为null

                    // 保存学生
                    studentService.saveStudent(student);
                    successCount++;

                } catch (Exception e) {
                    failCount++;
                    errorMessages.append("第").append(rowIndex + 1).append("行：").append(e.getMessage()).append("；");
                }
            }

            workbook.close();
            inputStream.close();

            // 构建返回消息
            StringBuilder result = new StringBuilder("success:成功导入").append(successCount).append("条");
            if (failCount > 0) {
                result.append("，失败").append(failCount).append("条");
                if (errorMessages.length() > 0) {
                    String errorMsg = errorMessages.toString();
                    // 限制错误消息长度
                    if (errorMsg.length() > 500) {
                        errorMsg = errorMsg.substring(0, 500) + "...";
                    }
                    result.append("。错误详情：").append(errorMsg);
                }
            }

            return result.toString();

        } catch (Exception e) {
            return "error:导入失败：" + e.getMessage();
        }
    }

    /**
     * 辅助方法：获取单元格的字符串值
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    // 处理数字，避免科学计数法
                    double numericValue = cell.getNumericCellValue();
                    if (numericValue == (long) numericValue) {
                        return String.valueOf((long) numericValue);
                    } else {
                        return String.valueOf(numericValue);
                    }
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
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
        
        // 检查是否是超级管理员
        User currentUser = (User) session.getAttribute("currentUser");
        boolean isSuperAdmin = currentUser != null && currentUser.getRole() != null && 
                               "SUPER_ADMIN".equals(currentUser.getRole().getName());
        
        Teacher teacher = getTeacherFromSession(session);
        Integer currentYear = configService.getCurrentDefenseYear();
        
        List<Student> students;
        if (isSuperAdmin) {
            // 超级管理员：返回所有学生
            if (currentYear == null) {
                students = studentService.findAll();
            } else {
                students = studentService.findByYear(currentYear);
            }
            result.put("teacherId", null);
            result.put("teacherName", "超级管理员");
        } else {
            // 普通教师：返回自己指导的学生
            if (teacher == null) {
                result.put("error", "请先登录教师账号");
                return result;
            }
            if (currentYear == null) {
                result.put("error", "请先设置当前答辩年份");
                return result;
            }
            students = studentService.getStudentsByAdvisor(teacher.getId(), currentYear);
            result.put("teacherId", teacher.getId());
            result.put("teacherName", teacher.getName());
        }
        
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
                // 设置院系信息
                if (s.getDepartment() != null) {
                    Map<String, Object> deptMap = new HashMap<>();
                    deptMap.put("name", s.getDepartment().getName());
                    info.put("department", deptMap);
                    info.put("departmentName", s.getDepartment().getName());
                } else {
                    info.put("department", null);
                    info.put("departmentName", null);
                }
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
        // 检查是否是超级管理员
        User currentUser = (User) session.getAttribute("currentUser");
        boolean isSuperAdmin = currentUser != null && currentUser.getRole() != null && 
                               "SUPER_ADMIN".equals(currentUser.getRole().getName());
        
        Teacher teacher = getTeacherFromSession(session);
        if (teacher == null && !isSuperAdmin) {
            return "error:请先登录教师账号";
        }
        
        Integer currentYear = configService.getCurrentDefenseYear();
        if (currentYear == null) {
            return "error:请先设置当前答辩年份";
        }
        
        // 验证该学生是否存在
        Student student = studentService.findById(studentId);
        if (student == null) {
            return "error:学生不存在";
        }
        
        // 如果不是超级管理员，验证该学生是否是当前教师指导的
        if (!isSuperAdmin && teacher != null) {
            if (student.getAdvisorTeacherId() == null || !student.getAdvisorTeacherId().equals(teacher.getId())) {
                return "error:您不是该学生的指导教师";
            }
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
        
        // 检查是否是超级管理员
        User currentUser = (User) session.getAttribute("currentUser");
        boolean isSuperAdmin = currentUser != null && currentUser.getRole() != null && 
                               "SUPER_ADMIN".equals(currentUser.getRole().getName());
        
        Teacher teacher = getTeacherFromSession(session);
        Integer currentYear = configService.getCurrentDefenseYear();
        
        List<Student> students;
        if (isSuperAdmin) {
            // 超级管理员：返回所有学生
            if (currentYear == null) {
                students = studentService.findAll();
            } else {
                students = studentService.findByYear(currentYear);
            }
            result.put("teacherId", null);
            result.put("teacherName", "超级管理员");
        } else {
            // 普通教师：返回自己作为评阅人的学生
            if (teacher == null) {
                result.put("error", "请先登录教师账号");
                return result;
            }
            if (currentYear == null) {
                result.put("error", "请先设置当前答辩年份");
                return result;
            }
            students = studentService.getStudentsByReviewer(teacher.getId(), currentYear);
            result.put("teacherId", teacher.getId());
            result.put("teacherName", teacher.getName());
        }
        
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
                // 设置院系信息
                if (s.getDepartment() != null) {
                    Map<String, Object> deptMap = new HashMap<>();
                    deptMap.put("name", s.getDepartment().getName());
                    info.put("department", deptMap);
                    info.put("departmentName", s.getDepartment().getName());
                } else {
                    info.put("department", null);
                    info.put("departmentName", null);
                }
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
                
                // 设置评阅人信息
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
        // 检查是否是超级管理员
        User currentUser = (User) session.getAttribute("currentUser");
        boolean isSuperAdmin = currentUser != null && currentUser.getRole() != null && 
                               "SUPER_ADMIN".equals(currentUser.getRole().getName());
        
        Teacher teacher = getTeacherFromSession(session);
        if (teacher == null && !isSuperAdmin) {
            return "error:请先登录教师账号";
        }
        
        Integer currentYear = configService.getCurrentDefenseYear();
        if (currentYear == null) {
            return "error:请先设置当前答辩年份";
        }
        
        // 验证该学生是否存在
        Student student = studentService.findById(studentId);
        if (student == null) {
            return "error:学生不存在";
        }
        
        // 如果不是超级管理员，验证该学生是否是当前教师评阅的
        if (!isSuperAdmin && teacher != null) {
            if (student.getReviewerTeacherId() == null || !student.getReviewerTeacherId().equals(teacher.getId())) {
                return "error:您不是该学生的评阅人";
            }
        }
        
        try {
            scoreService.setReviewerScore(studentId, currentYear, score);
            return "success";
        } catch (Exception e) {
            return "error:" + e.getMessage();
        }
    }
    
    /**
     * 获取学生的指导教师或评阅人的打分记录（用于超级管理员修改分项分数）
     * GET /department/student/teacher/scoreRecord?studentId=1&type=advisor/reviewer
     */
    @GetMapping("/teacher/scoreRecord")
    @ResponseBody
    public Map<String, Object> getTeacherScoreRecord(@RequestParam Long studentId, @RequestParam String type, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        
        // 检查是否是超级管理员
        User currentUser = (User) session.getAttribute("currentUser");
        boolean isSuperAdmin = currentUser != null && currentUser.getRole() != null && 
                               "SUPER_ADMIN".equals(currentUser.getRole().getName());
        
        if (!isSuperAdmin) {
            result.put("error", "权限不足");
            return result;
        }
        
        Integer currentYear = configService.getCurrentDefenseYear();
        if (currentYear == null) {
            result.put("error", "请先设置当前答辩年份");
            return result;
        }
        
        Student student = studentService.findById(studentId);
        if (student == null) {
            result.put("error", "学生不存在");
            return result;
        }
        
        Long teacherId = null;
        if ("advisor".equals(type)) {
            teacherId = student.getAdvisorTeacherId();
        } else if ("reviewer".equals(type)) {
            teacherId = student.getReviewerTeacherId();
        } else {
            result.put("error", "类型参数错误，应为advisor或reviewer");
            return result;
        }
        
        if (teacherId == null) {
            result.put("error", "该学生未分配" + ("advisor".equals(type) ? "指导教师" : "评阅人"));
            return result;
        }
        
        // 查找该教师的打分记录
        TeacherScoreRecord record = teacherScoreRecordMapper.findByStudentIdAndTeacherIdAndYear(studentId, teacherId, currentYear);
        if (record == null) {
            result.put("error", "未找到打分记录");
            return result;
        }
        
        result.put("record", record);
        result.put("student", student);
        Teacher teacher = teacherMapper.findById(teacherId);
        if (teacher != null) {
            result.put("teacherName", teacher.getName());
        }
        return result;
    }
    
    /**
     * 更新教师的打分记录（用于超级管理员和院系管理员修改分项分数）
     * POST /department/student/teacher/updateScoreRecord
     */
    @PostMapping("/teacher/updateScoreRecord")
    @ResponseBody
    public String updateTeacherScoreRecord(
            @RequestParam Long studentId,
            @RequestParam String type,
            @RequestParam(required = false) Integer item1,
            @RequestParam(required = false) Integer item2,
            @RequestParam(required = false) Integer item3,
            @RequestParam(required = false) Integer item4,
            @RequestParam(required = false) Integer item5,
            @RequestParam(required = false) Integer item6,
            HttpSession session) {
        
        // 检查是否是超级管理员或院系管理员
        String permissionError = checkDeptAdmin(session);
        if (permissionError != null) {
            return permissionError;
        }
        
        Integer currentYear = configService.getCurrentDefenseYear();
        if (currentYear == null) {
            return "error:请先设置当前答辩年份";
        }
        
        Student student = studentService.findById(studentId);
        if (student == null) {
            return "error:学生不存在";
        }
        
        Long teacherId = null;
        if ("advisor".equals(type)) {
            teacherId = student.getAdvisorTeacherId();
        } else if ("reviewer".equals(type)) {
            teacherId = student.getReviewerTeacherId();
        } else {
            return "error:类型参数错误";
        }
        
        if (teacherId == null) {
            return "error:该学生未分配" + ("advisor".equals(type) ? "指导教师" : "评阅人");
        }
        
        // 查找该教师的打分记录（如果没有记录，创建新记录）
        TeacherScoreRecord record = teacherScoreRecordMapper.findByStudentIdAndTeacherIdAndYear(studentId, teacherId, currentYear);
        boolean isNewRecord = false;
        if (record == null) {
            // 创建新记录
            record = new TeacherScoreRecord();
            record.setStudentId(studentId);
            record.setTeacherId(teacherId);
            record.setYear(currentYear);
            record.setDefenseGroupId(student.getDefenseGroupId());
            record.setSubmitTime(java.time.LocalDateTime.now());
            isNewRecord = true;
        }
        
        // 更新分项分数
        if (item1 != null) record.setItem1Score(item1);
        if (item2 != null) record.setItem2Score(item2);
        if (item3 != null) record.setItem3Score(item3);
        if (item4 != null) record.setItem4Score(item4);
        if (item5 != null) record.setItem5Score(item5);
        if (item6 != null) record.setItem6Score(item6);
        
        // 重新计算总分
        int total = (record.getItem1Score() != null ? record.getItem1Score() : 0) +
                   (record.getItem2Score() != null ? record.getItem2Score() : 0) +
                   (record.getItem3Score() != null ? record.getItem3Score() : 0) +
                   (record.getItem4Score() != null ? record.getItem4Score() : 0) +
                   (record.getItem5Score() != null ? record.getItem5Score() : 0) +
                   (record.getItem6Score() != null ? record.getItem6Score() : 0);
        record.setTotalScore(total);
        
        // 保存或更新记录
        try {
            if (isNewRecord) {
                teacherScoreRecordMapper.insert(record);
            } else {
                teacherScoreRecordMapper.update(record);
            }
            
            // 更新StudentFinalScore中的对应成绩
            if ("advisor".equals(type)) {
                scoreService.setAdvisorScore(studentId, currentYear, total);
            } else {
                scoreService.setReviewerScore(studentId, currentYear, total);
            }
            
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
    
    /**
     * 答辩组长：获取本组内所有教师给所有学生的打分情况
     * GET /department/student/leader/group/scores
     */
    @GetMapping("/leader/group/scores")
    @ResponseBody
    public Map<String, Object> getLeaderGroupScores(HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        
        // 获取当前登录的教师
        Teacher currentTeacher = (Teacher) session.getAttribute("currentTeacher");
        if (currentTeacher == null) {
            // 尝试从User获取Teacher
            User currentUser = (User) session.getAttribute("currentUser");
            if (currentUser != null && currentUser.getRole() != null && 
                ("TEACHER".equals(currentUser.getRole().getName()) || 
                 "DEFENSE_LEADER".equals(currentUser.getRole().getName()))) {
                currentTeacher = teacherMapper.findByUserId(currentUser.getId());
            }
        }
        
        if (currentTeacher == null) {
            result.put("error", "未登录或不是教师");
            return result;
        }
        
        // 查找该教师作为组长的小组
        com.example.defensemanagement.entity.DefenseGroupTeacher groupTeacher = defenseGroupTeacherMapper.findByTeacherId(currentTeacher.getId());
        if (groupTeacher == null || groupTeacher.getIsLeader() == null || groupTeacher.getIsLeader() != 1) {
            result.put("error", "您不是任何小组的组长");
            return result;
        }
        
        Long groupId = groupTeacher.getGroupId();
        Integer currentYear = configService.getCurrentDefenseYear();
        if (currentYear == null) {
            result.put("error", "请先设置当前答辩年份");
            return result;
        }
        
        // 获取该小组的所有学生
        List<Student> students = studentMapper.findByDefenseGroupId(groupId);
        students = students.stream()
            .filter(s -> currentYear.equals(s.getDefenseYear()))
            .collect(Collectors.toList());
        
        // 获取该小组的所有教师打分记录
        List<TeacherScoreRecord> allScores = teacherScoreRecordMapper.findByGroupIdAndYear(groupId, currentYear);
        
        // 构建返回数据：按学生分组，每个学生包含所有教师的打分
        List<Map<String, Object>> studentScoreList = new ArrayList<>();
        for (Student student : students) {
            Map<String, Object> studentInfo = new HashMap<>();
            studentInfo.put("studentId", student.getId());
            studentInfo.put("studentNo", student.getStudentNo());
            studentInfo.put("studentName", student.getName());
            studentInfo.put("classInfo", student.getClassInfo());
            studentInfo.put("defenseType", student.getDefenseType());
            studentInfo.put("title", student.getTitle());
            
            // 获取该学生的所有教师打分记录
            List<Map<String, Object>> teacherScores = new ArrayList<>();
            for (TeacherScoreRecord record : allScores) {
                if (record.getStudentId() != null && record.getStudentId().equals(student.getId())) {
                    Map<String, Object> scoreInfo = new HashMap<>();
                    scoreInfo.put("teacherId", record.getTeacherId());
                    if (record.getTeacher() != null) {
                        scoreInfo.put("teacherName", record.getTeacher().getName());
                        scoreInfo.put("teacherNo", record.getTeacher().getTeacherNo());
                    }
                    scoreInfo.put("item1Score", record.getItem1Score());
                    scoreInfo.put("item2Score", record.getItem2Score());
                    scoreInfo.put("item3Score", record.getItem3Score());
                    scoreInfo.put("item4Score", record.getItem4Score());
                    scoreInfo.put("item5Score", record.getItem5Score());
                    scoreInfo.put("item6Score", record.getItem6Score());
                    scoreInfo.put("totalScore", record.getTotalScore());
                    scoreInfo.put("submitTime", record.getSubmitTime());
                    teacherScores.add(scoreInfo);
                }
            }
            studentInfo.put("teacherScores", teacherScores);
            studentScoreList.add(studentInfo);
        }
        
        result.put("groupId", groupId);
        result.put("groupName", defenseGroupMapper.findById(groupId) != null ? 
                   defenseGroupMapper.findById(groupId).getName() : "");
        result.put("students", studentScoreList);
        result.put("year", currentYear);
        
        return result;
    }
}