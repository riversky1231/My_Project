package com.example.defensemanagement.controller;

import com.example.defensemanagement.entity.Student;
import com.example.defensemanagement.entity.StudentFinalScore;
import com.example.defensemanagement.entity.TeacherScoreRecord;
import com.example.defensemanagement.entity.DefenseGroupTeacher;
import com.example.defensemanagement.mapper.StudentFinalScoreMapper;
import com.example.defensemanagement.mapper.StudentMapper;
import com.example.defensemanagement.mapper.TeacherScoreRecordMapper;
import com.example.defensemanagement.mapper.DefenseGroupTeacherMapper;
import com.example.defensemanagement.mapper.TeacherMapper;
import com.example.defensemanagement.entity.Teacher;
import com.example.defensemanagement.entity.User;
import com.example.defensemanagement.service.DocTemplateService;
import javax.servlet.http.HttpSession;
import com.example.defensemanagement.service.ConfigService;
import com.example.defensemanagement.service.AiCommentService;
import com.example.defensemanagement.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * 简单导出接口：根据模板生成成绩相关 Word。
 * 模板需放在 classpath:templates/docx/ 下。
 */
@RestController
@RequestMapping("/export")
public class ExportController {

    private static final String PAPER_SCORE_TEMPLATE = "templates/docx/paper-score.docx";
    private static final String DESIGN_SCORE_TEMPLATE = "templates/docx/design-score.docx";
    private static final String PAPER_GRADE_TEMPLATE = "templates/docx/paper-grade.docx";
    private static final String DESIGN_GRADE_TEMPLATE = "templates/docx/design-grade.docx";
    private static final String PAPER_PROCESS_TEMPLATE = "templates/docx/paper-process.docx";
    private static final String DESIGN_PROCESS_TEMPLATE = "templates/docx/design-process.docx";
    private static final String GROUP_SUMMARY_TEMPLATE = "templates/docx/group-summary.docx";

    @Autowired
    private DocTemplateService docTemplateService;

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private TeacherScoreRecordMapper teacherScoreRecordMapper;

    @Autowired
    private StudentFinalScoreMapper studentFinalScoreMapper;

    @Autowired
    private ConfigService configService;

    @Autowired
    private AiCommentService aiCommentService;

    @Autowired
    private DefenseGroupTeacherMapper defenseGroupTeacherMapper;
    
    @Autowired
    private com.example.defensemanagement.mapper.DefenseGroupMapper defenseGroupMapper;
    
    @Autowired
    private TeacherMapper teacherMapper;
    
    @Autowired
    private UserService userService;

    @Value("${app.upload.base-dir:uploads}")
    private String uploadBaseDir;
    
    /**
     * 获取上传目录的绝对路径
     */
    private java.nio.file.Path getUploadBasePath() {
        java.nio.file.Path basePath = Paths.get(uploadBaseDir);
        // 如果是相对路径，转换为相对于项目根目录的绝对路径
        if (!basePath.isAbsolute()) {
            String userDir = System.getProperty("user.dir");
            basePath = Paths.get(userDir, uploadBaseDir);
        }
        return basePath;
    }

    @GetMapping("/score/paper/{studentId}")
    public ResponseEntity<?> exportPaperScore(@PathVariable Long studentId) {
        try {
            return buildScoreDoc(studentId, true);
        } catch (IllegalArgumentException e) {
            // 模板文件不存在等参数错误，返回JSON错误信息
            Map<String, Object> error = new HashMap<>();
            error.put("error", true);
            error.put("message", e.getMessage());
            error.put("type", "template_not_found");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            // 其他错误，返回JSON错误信息
            Map<String, Object> error = new HashMap<>();
            error.put("error", true);
            error.put("message", "导出论文成绩表失败: " + e.getMessage());
            error.put("type", "export_error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/score/design/{studentId}")
    public ResponseEntity<byte[]> exportDesignScore(@PathVariable Long studentId) {
        try {
            return buildScoreDoc(studentId, false);
        } catch (Exception e) {
            throw new RuntimeException("导出设计成绩表失败: " + e.getMessage(), e);
        }
    }

    @GetMapping("/grade/paper/{studentId}")
    public ResponseEntity<byte[]> exportPaperGrade(@PathVariable Long studentId) {
        return buildGradeDoc(studentId, true);
    }

    @GetMapping("/grade/design/{studentId}")
    public ResponseEntity<byte[]> exportDesignGrade(@PathVariable Long studentId) {
        return buildGradeDoc(studentId, false);
    }

    @GetMapping("/process/paper/{studentId}")
    public ResponseEntity<byte[]> exportPaperProcess(@PathVariable Long studentId) {
        return buildProcessDoc(studentId, true);
    }

    @GetMapping("/process/design/{studentId}")
    public ResponseEntity<byte[]> exportDesignProcess(@PathVariable Long studentId) {
        return buildProcessDoc(studentId, false);
    }

    /**
     * 打包导出小组全部学生成绩表（按学生 defenseType 选择模板）。
     */
    /**
     * 导出答辩小组统分表
     * GET /export/group/{groupId}/summary
     */
    @GetMapping("/group/{groupId}/summary")
    public ResponseEntity<byte[]> exportGroupSummary(@PathVariable Long groupId) {
        List<Student> students = studentMapper.findByDefenseGroupId(groupId);
        if (students == null || students.isEmpty()) {
            throw new RuntimeException("小组无学生");
        }
        
        // 获取当前年份（从第一个学生）
        Integer year = students.get(0).getDefenseYear();
        if (year == null) {
            throw new RuntimeException("学生未设置答辩年份");
        }
        
        // 获取小组信息
        com.example.defensemanagement.entity.DefenseGroup group = defenseGroupMapper.findById(groupId);
        String groupName = group != null ? group.getName() : "小组" + groupId;
        
        // 获取小组的所有评委（教师）
        List<DefenseGroupTeacher> groupTeachers = defenseGroupTeacherMapper.findByGroupId(groupId);
        
        // 获取院系名称（从第一个学生）
        String deptName = "未知院系";
        if (!students.isEmpty() && students.get(0).getDepartmentId() != null) {
            try {
                com.example.defensemanagement.entity.Department dept = 
                    userService.getAllDepartments().stream()
                        .filter(d -> d.getId().equals(students.get(0).getDepartmentId()))
                        .findFirst().orElse(null);
                if (dept != null) {
                    deptName = dept.getName();
                }
            } catch (Exception ignored) {}
        }
        
        // 构建统分表数据
        Map<String, String> ph = new HashMap<>();
        DateParts dp = getDateParts("DEFENSE_DATE");
        ph.put("{{YEAR}}", dp.year);
        ph.put("{{MONTH}}", dp.month);
        ph.put("{{DAY}}", dp.day);
        ph.put("{{GROUP_NAME}}", groupName);
        ph.put("{{DEPT_NAME}}", deptName);
        ph.put("{{GROUP_ID}}", String.valueOf(groupId));
        
        // 找出所有学生中评委数量最多的，确定需要多少列
        int maxJudges = 0;
        for (Student stu : students) {
            List<TeacherScoreRecord> records = teacherScoreRecordMapper.findByStudentIdAndYear(stu.getId(), year);
            if (records != null) {
                maxJudges = Math.max(maxJudges, records.size());
            }
        }
        // 至少要有1列，最多支持10列
        maxJudges = Math.max(1, Math.min(maxJudges, 10));
        ph.put("{{MAX_JUDGES}}", String.valueOf(maxJudges));
        
        // 构建学生数据行（支持动态评委列）
        int rowNum = 1;
        for (Student stu : students) {
            List<TeacherScoreRecord> records = teacherScoreRecordMapper.findByStudentIdAndYear(stu.getId(), year);
            StudentFinalScore fs = studentFinalScoreMapper.findByStudentIdAndYear(stu.getId(), year);
            
            // 计算平均分和每个评委的打分
            double avgScore = 0.0;
            int teacherCount = 0;
            List<Double> judgeScores = new java.util.ArrayList<>();
            
            if (records != null && !records.isEmpty()) {
                for (TeacherScoreRecord record : records) {
                    if (record.getTotalScore() != null) {
                        avgScore += record.getTotalScore();
                        teacherCount++;
                        judgeScores.add(record.getTotalScore().doubleValue());
                    }
                }
                if (teacherCount > 0) {
                    avgScore = avgScore / teacherCount;
                }
            }
            
            // 获取调节系数和最终得分
            double factor = fs != null && fs.getAdjustmentFactor() != null ? fs.getAdjustmentFactor() : 1.0;
            double finalScore = avgScore * factor;
            
            // 学生姓名
            ph.put("{{STU_NAME_" + rowNum + "}}", nvl(stu.getName()));
            ph.put("{{ROW_" + rowNum + "_NAME}}", nvl(stu.getName())); // 兼容旧格式
            
            // 每个评委的打分（动态列）
            for (int j = 1; j <= maxJudges; j++) {
                String score = "-";
                if (j <= judgeScores.size()) {
                    score = formatInt(judgeScores.get(j - 1));
                }
                ph.put("{{JUDGE_" + j + "_SCORE_" + rowNum + "}}", score);
            }
            
            // 答辩得分（平均分）
            ph.put("{{DEFENSE_SCORE_" + rowNum + "}}", format1(avgScore));
            ph.put("{{ROW_" + rowNum + "_AVG}}", format1(avgScore)); // 兼容旧格式
            
            // 调节系数
            ph.put("{{FACTOR_" + rowNum + "}}", String.format("%.3f", factor));
            ph.put("{{ROW_" + rowNum + "_FACTOR}}", String.format("%.3f", factor)); // 兼容旧格式
            
            // 最后得分
            ph.put("{{FINAL_SCORE_" + rowNum + "}}", format1(finalScore));
            ph.put("{{ROW_" + rowNum + "_FINAL}}", format1(finalScore)); // 兼容旧格式
            
            // 兼容旧格式：所有评委分数用"、"连接
            String teacherScores = "";
            for (int i = 0; i < judgeScores.size(); i++) {
                if (i > 0) teacherScores += "、";
                teacherScores += formatInt(judgeScores.get(i));
            }
            ph.put("{{ROW_" + rowNum + "_SCORES}}", teacherScores.isEmpty() ? "-" : teacherScores);
            
            rowNum++;
        }
        
        ph.put("{{TOTAL_ROWS}}", String.valueOf(rowNum - 1));
        
        // 渲染文档
        String filename = encode("毕业论文(设计)答辩小组统分表-" + groupName + ".docx");
        byte[] doc = docTemplateService.renderDoc(resolveTemplate("group-summary", GROUP_SUMMARY_TEMPLATE), ph, new HashMap<>());
        return attachment(doc, filename);
    }

    @GetMapping("/group/{groupId}/zip")
    public ResponseEntity<byte[]> exportGroupZip(@PathVariable Long groupId) {
        List<Student> list = studentMapper.findByDefenseGroupId(groupId);
        if (list == null || list.isEmpty()) {
            throw new RuntimeException("小组无学生");
        }
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (Student stu : list) {
                boolean isPaper = "PAPER".equalsIgnoreCase(stu.getDefenseType());
                ResponseEntity<byte[]> resp = buildScoreDoc(stu.getId(), isPaper);
                String filename = resp.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
                String cleanName = "student-" + stu.getId() + (isPaper ? "-paper.docx" : "-design.docx");
                if (filename != null && filename.contains("filename=\"")) {
                    int idx = filename.indexOf("filename=\"") + 10;
                    int end = filename.indexOf("\"", idx);
                    if (end > idx) {
                        cleanName = filename.substring(idx, end);
                    }
                }
                zos.putNextEntry(new ZipEntry(cleanName));
                zos.write(resp.getBody());
                zos.closeEntry();
            }
            zos.finish();
            byte[] zipBytes = baos.toByteArray();
            String zipName = encode("group-" + groupId + "-scores.zip");
            MediaType octet = MediaType.parseMediaType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + zipName + "\"")
                    .contentType(octet)
                    .body(zipBytes);
        } catch (Exception e) {
            throw new RuntimeException("打包导出失败: " + e.getMessage(), e);
        }
    }

    private ResponseEntity<byte[]> buildScoreDoc(Long studentId, boolean isPaper) {
        try {
            Student stu = studentMapper.findById(studentId);
            if (stu == null) throw new RuntimeException("学生不存在，ID: " + studentId);
            
            Integer year = stu.getDefenseYear();
            if (year == null) throw new RuntimeException("学生未设置答辩年份，ID: " + studentId);
            
            List<TeacherScoreRecord> records = teacherScoreRecordMapper.findByStudentIdAndYear(studentId, year);
            StudentFinalScore fs = studentFinalScoreMapper.findByStudentIdAndYear(studentId, year);
            double factor = fs != null && fs.getAdjustmentFactor() != null ? fs.getAdjustmentFactor() : 1.0;
            
            Map<String, String> ph = buildCommonPlaceholders(stu, getDateParts("DEFENSE_DATE"), isPaper);
            Map<String, byte[]> img = new HashMap<>();

            // 答辩成绩表：需要答辩组长签名（可选，没有签名也能导出）
            try {
                fillSignatures(img, stu, false, false);
            } catch (Exception e) {
                // 签名加载失败不影响导出，只记录日志
                System.err.println("警告：加载签名失败: " + e.getMessage());
            }

            String templatePath = resolveTemplate(isPaper ? "paper-score" : "design-score", 
                    isPaper ? PAPER_SCORE_TEMPLATE : DESIGN_SCORE_TEMPLATE);
            
            if (isPaper) {
                fillPaperScores(ph, records, factor, fs, stu);
                String filename = encode("本科毕业论文答辩成绩表-" + stu.getName() + ".docx");
                byte[] doc = docTemplateService.renderDoc(templatePath, ph, img);
                return attachment(doc, filename);
            } else {
                fillDesignScores(ph, records, factor, fs, stu);
                String filename = encode("本科毕业设计答辩成绩表-" + stu.getName() + ".docx");
                byte[] doc = docTemplateService.renderDoc(templatePath, ph, img);
                return attachment(doc, filename);
            }
        } catch (IllegalArgumentException e) {
            // 模板文件不存在的错误，提供更友好的提示
            throw new RuntimeException("导出失败: " + e.getMessage() + 
                "。请以超级管理员身份登录，在\"系统设置\"->\"模板管理\"中上传对应的Word模板文件。", e);
        } catch (Exception e) {
            throw new RuntimeException("导出失败: " + e.getMessage() + 
                " (学生ID: " + studentId + ", 类型: " + (isPaper ? "论文" : "设计") + ")", e);
        }
    }

    private ResponseEntity<byte[]> buildGradeDoc(Long studentId, boolean isPaper) {
        Student stu = studentMapper.findById(studentId);
        if (stu == null) throw new RuntimeException("学生不存在");
        Integer year = stu.getDefenseYear();
        StudentFinalScore fs = studentFinalScoreMapper.findByStudentIdAndYear(studentId, year);
        if (fs == null) throw new RuntimeException("未找到最终成绩");
        Map<String, String> ph = buildCommonPlaceholders(stu, getDateParts("GRADE_DATE"), isPaper);
        Map<String, byte[]> img = new HashMap<>();

        int advisorScore = fs.getAdvisorScore() == null ? 0 : fs.getAdvisorScore();
        int reviewerScore = fs.getReviewerScore() == null ? 0 : fs.getReviewerScore();
        double defenseScore = fs.getFinalDefenseScore() == null ? 0 : fs.getFinalDefenseScore();
        ph.put("{{ADVISOR_SCORE}}", String.valueOf(advisorScore));
        ph.put("{{REVIEWER_SCORE}}", String.valueOf(reviewerScore));
        ph.put("{{DEFENSE_SCORE}}", format1(defenseScore));
        ph.put("{{ADVISOR_30}}", format1(advisorScore * 0.3));
        ph.put("{{REVIEWER_30}}", format1(reviewerScore * 0.3));
        ph.put("{{DEFENSE_40}}", format1(defenseScore * 0.4));
        ph.put("{{TOTAL_GRADE}}", format1(fs.getTotalGrade() == null ? 0 : fs.getTotalGrade()));

        // 成绩评定表：需要评语（论文和设计类型都需要评语）
        List<TeacherScoreRecord> records = teacherScoreRecordMapper.findByStudentIdAndYear(studentId, year);
        double factor = fs.getAdjustmentFactor() != null ? fs.getAdjustmentFactor() : 1.0;
        if (isPaper) {
            // 论文类型：需要评语
            AvgScores avg = avgPaper(records);
            double item1Scaled = avg.item1Scaled(factor);
            double item2Scaled = avg.item2Scaled(factor);
            double item3Scaled = avg.item3Scaled(factor);
            double totalFromItems = item1Scaled + item2Scaled + item3Scaled;
            ph.put("{{COMMENT}}", generateComment("PAPER_PROMPT", stu, totalFromItems, factor));
        } else {
            // 设计类型：需要评语
            double totalScore = fs.getFinalDefenseScore() != null ? fs.getFinalDefenseScore() : 0.0;
            ph.put("{{COMMENT}}", generateComment("DESIGN_PROMPT", stu, totalScore, factor));
        }

        // 成绩评定表：需要指导教师、评阅教师、答辩组长和系主任签名
        try {
            fillSignatures(img, stu, true, false);
        } catch (Exception e) {
            // 签名加载失败不影响导出，只记录日志
            System.err.println("警告：加载签名失败: " + e.getMessage());
        }
        
        String filename = encode((isPaper ? "本科毕业论文成绩评定表-" : "本科毕业设计成绩评定表-") + stu.getName() + ".docx");
        String template = isPaper ? resolveTemplate("paper-grade", PAPER_GRADE_TEMPLATE)
                : resolveTemplate("design-grade", DESIGN_GRADE_TEMPLATE);
        byte[] doc = docTemplateService.renderDoc(template, ph, img);
        return attachment(doc, filename);
    }

    /**
     * 答辩组长：获取组内学生列表（用于查看成绩评定表）
     * GET /export/leader/students
     */
    @GetMapping("/leader/students")
    public ResponseEntity<?> getLeaderStudents(HttpSession session) {
        try {
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
                Map<String, Object> error = new HashMap<>();
                error.put("error", "未登录或不是教师");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }
            
            // 查找该教师作为组长的小组
            List<DefenseGroupTeacher> allGroups = defenseGroupTeacherMapper.findAll();
            Long groupId = null;
            for (DefenseGroupTeacher gt : allGroups) {
                if (gt.getTeacherId() != null && gt.getTeacherId().equals(currentTeacher.getId()) && 
                    gt.getIsLeader() != null && gt.getIsLeader() == 1) {
                    groupId = gt.getGroupId();
                    break;
                }
            }
            
            if (groupId == null) {
                Map<String, Object> result = new HashMap<>();
                result.put("students", new java.util.ArrayList<>());
                result.put("message", "您不是任何小组的组长");
                return ResponseEntity.ok(result);
            }
            
            // 获取该小组的所有学生
            List<Student> students = studentMapper.findByDefenseGroupId(groupId);
            Integer currentYear = configService.getCurrentDefenseYear();
            if (currentYear != null) {
                students = students.stream()
                    .filter(s -> currentYear.equals(s.getDefenseYear()))
                    .collect(java.util.stream.Collectors.toList());
            }
            
            // 转换为简化的学生信息（用于前端显示）
            List<Map<String, Object>> studentList = students.stream().map(s -> {
                Map<String, Object> info = new HashMap<>();
                info.put("id", s.getId());
                info.put("name", s.getName());
                info.put("studentNo", s.getStudentNo());
                info.put("title", s.getTitle());
                info.put("defenseType", s.getDefenseType());
                // 检查是否有最终成绩（用于判断是否可以导出成绩评定表）
                StudentFinalScore fs = studentFinalScoreMapper.findByStudentIdAndYear(s.getId(), s.getDefenseYear());
                info.put("hasFinalScore", fs != null);
                return info;
            }).collect(java.util.stream.Collectors.toList());
            
            Map<String, Object> result = new HashMap<>();
            result.put("students", studentList);
            result.put("groupId", groupId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "获取学生列表失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 教师：获取自己所带学生的列表（用于查看成绩评定表）
     * GET /export/teacher/students
     */
    @GetMapping("/teacher/students")
    public ResponseEntity<?> getTeacherStudents(HttpSession session) {
        try {
            // 获取当前登录的教师
            Teacher currentTeacher = (Teacher) session.getAttribute("currentTeacher");
            if (currentTeacher == null) {
                User currentUser = (User) session.getAttribute("currentUser");
                if (currentUser != null && currentUser.getRole() != null && 
                    "TEACHER".equals(currentUser.getRole().getName())) {
                    currentTeacher = teacherMapper.findByUserId(currentUser.getId());
                }
            }
            
            if (currentTeacher == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "未登录或不是教师");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }
            
            Integer currentYear = configService.getCurrentDefenseYear();
            // 获取该教师指导的所有学生
            List<Student> students = studentMapper.findByAdvisorIdAndYear(currentTeacher.getId(), currentYear);
            
            // 转换为简化的学生信息
            List<Map<String, Object>> studentList = students.stream().map(s -> {
                Map<String, Object> info = new HashMap<>();
                info.put("id", s.getId());
                info.put("name", s.getName());
                info.put("studentNo", s.getStudentNo());
                info.put("title", s.getTitle());
                info.put("defenseType", s.getDefenseType());
                // 检查是否有最终成绩
                StudentFinalScore fs = studentFinalScoreMapper.findByStudentIdAndYear(s.getId(), s.getDefenseYear());
                info.put("hasFinalScore", fs != null);
                return info;
            }).collect(java.util.stream.Collectors.toList());
            
            Map<String, Object> result = new HashMap<>();
            result.put("students", studentList);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "获取学生列表失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 教师：一键打包本组所有学生的成绩评定表（自己所指导的学生）
     * GET /export/teacher/grade/zip
     */
    @GetMapping("/teacher/grade/zip")
    public ResponseEntity<?> exportTeacherGradeZip(HttpSession session) {
        try {
            // 获取当前登录的教师
            Teacher currentTeacher = (Teacher) session.getAttribute("currentTeacher");
            if (currentTeacher == null) {
                User currentUser = (User) session.getAttribute("currentUser");
                if (currentUser != null && currentUser.getRole() != null && 
                    "TEACHER".equals(currentUser.getRole().getName())) {
                    currentTeacher = teacherMapper.findByUserId(currentUser.getId());
                }
            }
            
            if (currentTeacher == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "未登录或不是教师");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }
            
            Integer currentYear = configService.getCurrentDefenseYear();
            // 获取该教师指导的所有学生
            List<Student> students = studentMapper.findByAdvisorIdAndYear(currentTeacher.getId(), currentYear);
            
            // 过滤出有最终成绩的学生
            students = students.stream().filter(s -> {
                StudentFinalScore fs = studentFinalScoreMapper.findByStudentIdAndYear(s.getId(), s.getDefenseYear());
                return fs != null;
            }).collect(java.util.stream.Collectors.toList());
            
            if (students.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "没有可导出的学生成绩评定表");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }
            
            // 打包所有学生的成绩评定表
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                 ZipOutputStream zos = new ZipOutputStream(baos)) {
                for (Student stu : students) {
                    boolean isPaper = "PAPER".equalsIgnoreCase(stu.getDefenseType());
                    ResponseEntity<byte[]> resp = buildGradeDoc(stu.getId(), isPaper);
                    String filename = resp.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
                    String cleanName = (isPaper ? "本科毕业论文成绩评定表-" : "本科毕业设计成绩评定表-") + stu.getName() + ".docx";
                    if (filename != null && filename.contains("filename=\"")) {
                        int idx = filename.indexOf("filename=\"") + 10;
                        int end = filename.indexOf("\"", idx);
                        if (end > idx) {
                            cleanName = filename.substring(idx, end);
                        }
                    }
                    zos.putNextEntry(new ZipEntry(cleanName));
                    zos.write(resp.getBody());
                    zos.closeEntry();
                }
                zos.finish();
                byte[] zipBytes = baos.toByteArray();
                String zipName = encode("教师-" + currentTeacher.getName() + "-成绩评定表.zip");
                MediaType octet = MediaType.parseMediaType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + zipName + "\"")
                        .contentType(octet)
                        .body(zipBytes);
            }
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "打包导出失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 教师：一键打包下载评分过的所有本组学生的成绩评定表
     * GET /export/teacher/group/graded/zip
     */
    @GetMapping("/teacher/group/graded/zip")
    public ResponseEntity<?> exportTeacherGradedGroupStudentsZip(HttpSession session) {
        try {
            // 获取当前登录的教师
            Teacher currentTeacher = (Teacher) session.getAttribute("currentTeacher");
            if (currentTeacher == null) {
                User currentUser = (User) session.getAttribute("currentUser");
                if (currentUser != null && currentUser.getRole() != null && 
                    "TEACHER".equals(currentUser.getRole().getName())) {
                    currentTeacher = teacherMapper.findByUserId(currentUser.getId());
                }
            }
            
            if (currentTeacher == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "未登录或不是教师");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }
            
            // 提取teacherId为final变量，供lambda表达式使用
            final Long teacherId = currentTeacher.getId();
            
            // 查找该教师所在的小组
            DefenseGroupTeacher groupTeacher = defenseGroupTeacherMapper.findByTeacherId(teacherId);
            if (groupTeacher == null || groupTeacher.getGroupId() == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "您不在任何答辩小组中");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }
            
            Long groupId = groupTeacher.getGroupId();
            Integer currentYear = configService.getCurrentDefenseYear();
            
            // 获取该小组的所有学生
            List<Student> groupStudents = studentMapper.findByDefenseGroupId(groupId);
            if (currentYear != null) {
                final Integer year = currentYear; // 提取为final变量
                groupStudents = groupStudents.stream()
                    .filter(s -> year.equals(s.getDefenseYear()))
                    .collect(java.util.stream.Collectors.toList());
            }
            
            // 过滤出该教师已经评分过的学生（有TeacherScoreRecord记录）
            List<Student> gradedStudents = groupStudents.stream().filter(s -> {
                List<TeacherScoreRecord> records = teacherScoreRecordMapper.findByStudentIdAndYear(s.getId(), s.getDefenseYear());
                if (records == null || records.isEmpty()) {
                    return false;
                }
                // 检查是否有该教师的评分记录
                return records.stream().anyMatch(r -> 
                    r.getTeacherId() != null && r.getTeacherId().equals(teacherId));
            }).collect(java.util.stream.Collectors.toList());
            
            // 进一步过滤：只导出有最终成绩的学生（可以导出成绩评定表）
            gradedStudents = gradedStudents.stream().filter(s -> {
                StudentFinalScore fs = studentFinalScoreMapper.findByStudentIdAndYear(s.getId(), s.getDefenseYear());
                return fs != null;
            }).collect(java.util.stream.Collectors.toList());
            
            if (gradedStudents.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "没有可导出的已评分学生成绩评定表");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }
            
            // 打包所有已评分学生的成绩评定表
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                 ZipOutputStream zos = new ZipOutputStream(baos)) {
                for (Student stu : gradedStudents) {
                    boolean isPaper = "PAPER".equalsIgnoreCase(stu.getDefenseType());
                    ResponseEntity<byte[]> resp = buildGradeDoc(stu.getId(), isPaper);
                    String filename = resp.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
                    String cleanName = (isPaper ? "本科毕业论文成绩评定表-" : "本科毕业设计成绩评定表-") + stu.getName() + ".docx";
                    if (filename != null && filename.contains("filename=\"")) {
                        int idx = filename.indexOf("filename=\"") + 10;
                        int end = filename.indexOf("\"", idx);
                        if (end > idx) {
                            cleanName = filename.substring(idx, end);
                        }
                    }
                    zos.putNextEntry(new ZipEntry(cleanName));
                    zos.write(resp.getBody());
                    zos.closeEntry();
                }
                zos.finish();
                byte[] zipBytes = baos.toByteArray();
                String zipName = encode("教师-" + currentTeacher.getName() + "-本组已评分学生成绩评定表.zip");
                MediaType octet = MediaType.parseMediaType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + zipName + "\"")
                        .contentType(octet)
                        .body(zipBytes);
            }
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "打包导出失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    private ResponseEntity<byte[]> buildProcessDoc(Long studentId, boolean isPaper) {
        Student stu = studentMapper.findById(studentId);
        if (stu == null) throw new RuntimeException("学生不存在");
        Integer year = stu.getDefenseYear();
        List<TeacherScoreRecord> records = teacherScoreRecordMapper.findByStudentIdAndYear(studentId, year);
        StudentFinalScore fs = studentFinalScoreMapper.findByStudentIdAndYear(studentId, year);
        double factor = fs != null && fs.getAdjustmentFactor() != null ? fs.getAdjustmentFactor() : 1.0;

        Map<String, String> ph = buildCommonPlaceholders(stu, getDateParts("DEFENSE_DATE"), isPaper);
        Map<String, byte[]> img = new HashMap<>();
        // 过程表：需要评委签名
        fillSignatures(img, stu, false, true);
        if (isPaper) {
            fillPaperScores(ph, records, factor, fs, stu);
        } else {
            fillDesignScores(ph, records, factor, fs, stu);
        }
        String filename = encode((isPaper ? "毕业论文答辩成绩无评语过程表-" : "毕业设计答辩成绩无评语过程表-") + stu.getName() + ".docx");
        String template = isPaper ? resolveTemplate("paper-process", PAPER_PROCESS_TEMPLATE)
                : resolveTemplate("design-process", DESIGN_PROCESS_TEMPLATE);
        byte[] doc = docTemplateService.renderDoc(template, ph, img);
        return attachment(doc, filename);
    }

    private Map<String, String> buildCommonPlaceholders(Student stu, DateParts dp, boolean isPaper) {
        Map<String, String> ph = new HashMap<>();
        ph.put("{{NAME}}", nvl(stu.getName()));
        ph.put("{{STUDENT_NO}}", nvl(stu.getStudentNo()));
        ph.put("{{TITLE}}", nvl(stu.getTitle()));
        ph.put("{{YEAR}}", dp.year);
        ph.put("{{MONTH}}", dp.month);
        ph.put("{{DAY}}", dp.day);
        if (!isPaper) {
            // 设计表有摘要/类型可按需加入
            ph.put("{{SUMMARY}}", nvl(stu.getSummary()));
        }
        return ph;
    }

    private void fillPaperScores(Map<String, String> ph, List<TeacherScoreRecord> records, double factor, StudentFinalScore fs, Student stu) {
        AvgScores avg = avgPaper(records);
        // 计算各个分项（应用调节系数）
        double item1Scaled = avg.item1Scaled(factor);
        double item2Scaled = avg.item2Scaled(factor);
        double item3Scaled = avg.item3Scaled(factor);
        
        // 总分应该等于各个分项的和（确保一致性）
        double totalFromItems = item1Scaled + item2Scaled + item3Scaled;
        
        ph.put("{{ITEM1}}", formatInt(item1Scaled));
        ph.put("{{ITEM2}}", formatInt(item2Scaled));
        ph.put("{{ITEM3}}", formatInt(item3Scaled));
        // 总分使用各个分项的和，确保一致性
        ph.put("{{TOTAL}}", format1(totalFromItems));
        ph.put("{{COMMENT}}", generateComment("PAPER_PROMPT", stu, totalFromItems, factor));
    }

    private void fillDesignScores(Map<String, String> ph, List<TeacherScoreRecord> records, double factor, StudentFinalScore fs, Student stu) {
        AvgScores avg = avgDesign(records);
        // 计算各个分项（应用调节系数）
        double item1Scaled = avg.item1Scaled(factor);
        double item2Scaled = avg.item2Scaled(factor);
        double item3Scaled = avg.item3Scaled(factor);
        double item4Scaled = avg.item4Scaled(factor);
        double item5Scaled = avg.item5Scaled(factor);
        double item6Scaled = avg.item6Scaled(factor);
        
        // 总分应该等于各个分项的和（确保一致性）
        double totalFromItems = item1Scaled + item2Scaled + item3Scaled + item4Scaled + item5Scaled + item6Scaled;
        
        ph.put("{{ITEM1}}", formatInt(item1Scaled));
        ph.put("{{ITEM2}}", formatInt(item2Scaled));
        ph.put("{{ITEM3}}", formatInt(item3Scaled));
        ph.put("{{ITEM4}}", formatInt(item4Scaled));
        ph.put("{{ITEM5}}", formatInt(item5Scaled));
        ph.put("{{ITEM6}}", formatInt(item6Scaled));
        // 总分使用各个分项的和，确保一致性
        ph.put("{{TOTAL}}", format1(totalFromItems));
        ph.put("{{COMMENT}}", generateComment("DESIGN_PROMPT", stu, totalFromItems, factor));
    }

    private AvgScores avgPaper(List<TeacherScoreRecord> records) {
        AvgScores s = new AvgScores();
        if (records == null || records.isEmpty()) return s;
        int n = 0;
        for (TeacherScoreRecord r : records) {
            if (r.getTotalScore() == null) continue;
            n++;
            s.item1 += nz(r.getItem1Score());
            s.item2 += nz(r.getItem2Score());
            s.item3 += nz(r.getItem3Score());
            s.total += nz(r.getTotalScore());
        }
        if (n > 0) s.divide(n);
        return s;
    }

    private AvgScores avgDesign(List<TeacherScoreRecord> records) {
        AvgScores s = new AvgScores();
        if (records == null || records.isEmpty()) return s;
        int n = 0;
        for (TeacherScoreRecord r : records) {
            if (r.getTotalScore() == null) continue;
            n++;
            s.item1 += nz(r.getItem1Score());
            s.item2 += nz(r.getItem2Score());
            s.item3 += nz(r.getItem3Score());
            s.item4 += nz(r.getItem4Score());
            s.item5 += nz(r.getItem5Score());
            s.item6 += nz(r.getItem6Score());
            s.total += nz(r.getTotalScore());
        }
        if (n > 0) s.divide(n);
        return s;
    }

    private DateParts getDateParts(String prefix) {
        String y = configService.getDefenseDatePart(prefix + "_YEAR");
        String m = configService.getDefenseDatePart(prefix + "_MONTH");
        String d = configService.getDefenseDatePart(prefix + "_DAY");
        LocalDate now = LocalDate.now();
        return new DateParts(nvl(y, String.valueOf(now.getYear())),
                nvl(m, String.valueOf(now.getMonthValue())),
                nvl(d, String.valueOf(now.getDayOfMonth())));
    }

    private ResponseEntity<byte[]> attachment(byte[] bytes, String filename) {
        MediaType octet = MediaType.parseMediaType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(octet)
                .body(bytes);
    }

    private String encode(String name) {
        try {
            return URLEncoder.encode(name, StandardCharsets.UTF_8.toString()).replaceAll("\\+", "%20");
        } catch (Exception e) {
            return name;
        }
    }

    private String nvl(String v) { return v == null ? "" : v; }

    private String nvl(String v, String def) { return StringUtils.hasText(v) ? v : def; }

    private int nz(Integer v) { return v == null ? 0 : v; }

    private String format1(double v) { return String.format(java.util.Locale.ROOT, "%.1f", v); }

    private String formatInt(double v) { return String.valueOf(Math.round(v)); }

    private String generateComment(String promptKey, Student stu, double scaledTotal, double factor) {
        String context = "学生:" + nvl(stu.getName()) +
                "，题目:" + nvl(stu.getTitle()) +
                "，摘要:" + nvl(stu.getSummary()) +
                "，调节系数:" + format1(factor) +
                "，小组加权总分:" + format1(scaledTotal);
        String resp = aiCommentService.generateComment(promptKey, context);
        return resp == null ? "" : resp;
    }

    /**
     * 填充签名图片
     * @param img 图片映射
     * @param stu 学生信息
     * @param isGradeForm 是否为成绩评定表（需要系主任签名）
     * @param isProcessForm 是否为过程表（需要评委签名）
     */
    private void fillSignatures(Map<String, byte[]> img, Student stu, boolean isGradeForm, boolean isProcessForm) {
        // 指导教师签名
        if (stu.getAdvisorTeacherId() != null) {
            byte[] bytes = loadSignature("teacher_" + stu.getAdvisorTeacherId());
            if (bytes != null) {
                img.put("{{SIGN_ADVISOR}}", bytes);
                img.put("{{SIGN_ADVISOR_TEACHER}}", bytes); // 兼容性占位符
                System.out.println("已加载指导教师签名: teacher_" + stu.getAdvisorTeacherId() + ", 大小: " + bytes.length);
            } else {
                System.out.println("警告：未找到指导教师签名: teacher_" + stu.getAdvisorTeacherId());
            }
        }
        
        // 评阅教师签名
        if (stu.getReviewerTeacherId() != null) {
            byte[] bytes = loadSignature("teacher_" + stu.getReviewerTeacherId());
            if (bytes != null) {
                img.put("{{SIGN_REVIEWER}}", bytes);
                img.put("{{SIGN_REVIEWER_TEACHER}}", bytes); // 兼容性占位符
                System.out.println("已加载评阅教师签名: teacher_" + stu.getReviewerTeacherId() + ", 大小: " + bytes.length);
            } else {
                System.out.println("警告：未找到评阅教师签名: teacher_" + stu.getReviewerTeacherId());
            }
        }
        
        // 答辩组长签名（用于答辩成绩表和成绩评定表）
        if (stu.getDefenseGroupId() != null) {
            try {
                DefenseGroupTeacher leader = defenseGroupTeacherMapper.findLeaderByGroupId(stu.getDefenseGroupId());
                if (leader != null && leader.getTeacherId() != null) {
                    byte[] bytes = loadSignature("teacher_" + leader.getTeacherId());
                    if (bytes != null) {
                        img.put("{{SIGN_LEADER}}", bytes);
                        img.put("{{SIGN_GROUP_LEADER}}", bytes); // 兼容性占位符
                        System.out.println("已加载答辩组长签名: teacher_" + leader.getTeacherId() + ", 大小: " + bytes.length);
                    } else {
                        System.out.println("警告：未找到答辩组长签名: teacher_" + leader.getTeacherId());
                    }
                } else {
                    System.out.println("警告：未找到答辩组长，小组ID: " + stu.getDefenseGroupId());
                }
            } catch (Exception e) {
                System.out.println("警告：加载答辩组长签名时出错: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        // 系主任签名（用于成绩评定表）
        if (isGradeForm && stu.getDepartmentId() != null) {
            try {
                // 查找该院系的院系管理员（DEPT_ADMIN角色）作为系主任
                List<com.example.defensemanagement.entity.User> deptAdmins = 
                    userService.getUsersByRole("DEPT_ADMIN");
                for (com.example.defensemanagement.entity.User admin : deptAdmins) {
                    if (admin.getDepartmentId() != null && 
                        admin.getDepartmentId().equals(stu.getDepartmentId())) {
                        byte[] bytes = loadSignature("user_" + admin.getId());
                        if (bytes != null) {
                            img.put("{{SIGN_DEPT_HEAD}}", bytes);
                            img.put("{{SIGN_DEAN}}", bytes); // 兼容性占位符
                            break; // 只取第一个找到的
                        }
                    }
                }
            } catch (Exception ignored) {}
        }
        
        // 评委签名（用于过程表，可能需要多个评委的签名）
        if (isProcessForm && stu.getDefenseGroupId() != null) {
            try {
                // 获取该小组的所有教师（评委）
                List<DefenseGroupTeacher> groupTeachers = 
                    defenseGroupTeacherMapper.findByGroupId(stu.getDefenseGroupId());
                int judgeIndex = 1;
                for (DefenseGroupTeacher groupTeacher : groupTeachers) {
                    if (groupTeacher.getTeacherId() != null) {
                        byte[] bytes = loadSignature("teacher_" + groupTeacher.getTeacherId());
                        if (bytes != null) {
                            // 支持多个评委签名：{{SIGN_JUDGE_1}}, {{SIGN_JUDGE_2}}, {{SIGN_JUDGE_3}}
                            img.put("{{SIGN_JUDGE_" + judgeIndex + "}}", bytes);
                            // 第一个评委也可以使用通用占位符
                            if (judgeIndex == 1) {
                                img.put("{{SIGN_JUDGE}}", bytes);
                            }
                            judgeIndex++;
                            // 最多支持3个评委签名
                            if (judgeIndex > 3) break;
                        }
                    }
                }
            } catch (Exception ignored) {}
        }
    }
    
    /**
     * 兼容旧版本的签名填充方法（默认不是成绩评定表，不是过程表）
     */
    private void fillSignatures(Map<String, byte[]> img, Student stu) {
        fillSignatures(img, stu, false, false);
    }

    private byte[] loadSignature(String namePrefix) {
        String[] exts = {"png", "jpg", "jpeg"};
        java.nio.file.Path basePath = getUploadBasePath();
        
        // 首先尝试直接使用传入的前缀（如 teacher_1）
        for (String ext : exts) {
            java.nio.file.Path p = basePath.resolve("signatures").resolve(namePrefix + "." + ext);
            if (Files.exists(p)) {
                try {
                    byte[] bytes = Files.readAllBytes(p);
                    System.out.println("找到签名文件: " + p.toString() + ", 大小: " + bytes.length);
                    return bytes;
                } catch (Exception e) {
                    System.out.println("读取签名文件失败: " + p.toString() + ", 错误: " + e.getMessage());
                }
            } else {
                System.out.println("签名文件不存在: " + p.toString());
            }
        }
        
        // 如果是teacher_前缀但没找到签名，尝试查找对应的user_签名
        // 因为教师登录时可能以User身份登录，签名保存为user_{userId}格式
        if (namePrefix.startsWith("teacher_")) {
            try {
                Long teacherId = Long.parseLong(namePrefix.substring("teacher_".length()));
                Teacher teacher = teacherMapper.findById(teacherId);
                if (teacher != null && teacher.getUserId() != null) {
                    String userPrefix = "user_" + teacher.getUserId();
                    System.out.println("尝试查找user_格式签名: " + userPrefix);
                    for (String ext : exts) {
                        java.nio.file.Path p = basePath.resolve("signatures").resolve(userPrefix + "." + ext);
                        if (Files.exists(p)) {
                            try {
                                byte[] bytes = Files.readAllBytes(p);
                                System.out.println("找到user_格式签名文件: " + p.toString() + ", 大小: " + bytes.length);
                                return bytes;
                            } catch (Exception e) {
                                System.out.println("读取user_格式签名文件失败: " + p.toString() + ", 错误: " + e.getMessage());
                            }
                        } else {
                            System.out.println("user_格式签名文件不存在: " + p.toString());
                        }
                    }
                } else {
                    System.out.println("教师不存在或没有关联的userId: teacherId=" + teacherId);
                }
            } catch (NumberFormatException e) {
                System.out.println("解析teacherId失败: " + namePrefix);
            }
        }
        
        System.out.println("未找到签名: " + namePrefix);
        return null;
    }

    private String resolveTemplate(String key, String defaultClasspath) {
        // 优先使用上传的模板（使用绝对路径）
        java.nio.file.Path basePath = getUploadBasePath();
        java.nio.file.Path p = basePath.resolve("templates").resolve(key + ".docx");
        if (Files.exists(p)) {
            return p.toAbsolutePath().toString();
        }
        // 如果上传的模板不存在，尝试使用classpath中的默认模板
        // 注意：如果默认模板也不存在，会在DocTemplateService中抛出异常
        return defaultClasspath;
    }

    private static class DateParts {
        String year; String month; String day;
        DateParts(String y, String m, String d) { this.year = y; this.month = m; this.day = d; }
    }

    private static class AvgScores {
        double item1, item2, item3, item4, item5, item6, total;
        void divide(int n) {
            item1 /= n; item2 /= n; item3 /= n; item4 /= n; item5 /= n; item6 /= n; total /= n;
        }
        double item1Scaled(double f) { return item1 * f; }
        double item2Scaled(double f) { return item2 * f; }
        double item3Scaled(double f) { return item3 * f; }
        double item4Scaled(double f) { return item4 * f; }
        double item5Scaled(double f) { return item5 * f; }
        double item6Scaled(double f) { return item6 * f; }
        double totalScaled(double f) { return total * f; }
    }
}

