package com.example.defensemanagement.controller;

import com.example.defensemanagement.entity.Student;
import com.example.defensemanagement.entity.StudentFinalScore;
import com.example.defensemanagement.entity.TeacherScoreRecord;
import com.example.defensemanagement.entity.DefenseGroupTeacher;
import com.example.defensemanagement.mapper.StudentFinalScoreMapper;
import com.example.defensemanagement.mapper.StudentMapper;
import com.example.defensemanagement.mapper.TeacherScoreRecordMapper;
import com.example.defensemanagement.mapper.DefenseGroupTeacherMapper;
import com.example.defensemanagement.service.DocTemplateService;
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
        
        // 构建统分表数据
        Map<String, String> ph = new HashMap<>();
        DateParts dp = getDateParts("DEFENSE_DATE");
        ph.put("{{YEAR}}", dp.year);
        ph.put("{{MONTH}}", dp.month);
        ph.put("{{DAY}}", dp.day);
        ph.put("{{GROUP_ID}}", String.valueOf(groupId));
        
        // 构建学生数据行
        StringBuilder studentRows = new StringBuilder();
        int rowNum = 1;
        
        for (Student stu : students) {
            List<TeacherScoreRecord> records = teacherScoreRecordMapper.findByStudentIdAndYear(stu.getId(), year);
            StudentFinalScore fs = studentFinalScoreMapper.findByStudentIdAndYear(stu.getId(), year);
            
            // 计算平均分（每个评委的打分）
            double avgScore = 0.0;
            int teacherCount = 0;
            String teacherScores = "";
            
            if (records != null && !records.isEmpty()) {
                for (TeacherScoreRecord record : records) {
                    if (record.getTotalScore() != null) {
                        avgScore += record.getTotalScore();
                        teacherCount++;
                        if (!teacherScores.isEmpty()) teacherScores += "、";
                        teacherScores += String.valueOf(record.getTotalScore());
                    }
                }
                if (teacherCount > 0) {
                    avgScore = avgScore / teacherCount;
                }
            }
            
            // 获取调节系数和最终得分
            double factor = fs != null && fs.getAdjustmentFactor() != null ? fs.getAdjustmentFactor() : 1.0;
            double finalScore = avgScore * factor;
            
            // 构建行数据（使用占位符，实际模板中需要替换）
            studentRows.append("{{ROW_").append(rowNum).append("_NAME}}|")
                      .append("{{ROW_").append(rowNum).append("_SCORES}}|")
                      .append("{{ROW_").append(rowNum).append("_AVG}}|")
                      .append("{{ROW_").append(rowNum).append("_FACTOR}}|")
                      .append("{{ROW_").append(rowNum).append("_FINAL}}");
            
            ph.put("{{ROW_" + rowNum + "_NAME}}", nvl(stu.getName()));
            ph.put("{{ROW_" + rowNum + "_SCORES}}", teacherScores.isEmpty() ? "-" : teacherScores);
            ph.put("{{ROW_" + rowNum + "_AVG}}", format1(avgScore));
            ph.put("{{ROW_" + rowNum + "_FACTOR}}", String.format("%.3f", factor));
            ph.put("{{ROW_" + rowNum + "_FINAL}}", format1(finalScore));
            
            rowNum++;
        }
        
        ph.put("{{TOTAL_ROWS}}", String.valueOf(rowNum - 1));
        
        // 渲染文档
        String filename = encode("毕业论文(设计)答辩小组统分表-小组" + groupId + ".docx");
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

        // 成绩评定表：需要系主任签名
        fillSignatures(img, stu, true, false);
        String filename = encode((isPaper ? "本科毕业论文成绩评定表-" : "本科毕业设计成绩评定表-") + stu.getName() + ".docx");
        String template = isPaper ? resolveTemplate("paper-grade", PAPER_GRADE_TEMPLATE)
                : resolveTemplate("design-grade", DESIGN_GRADE_TEMPLATE);
        byte[] doc = docTemplateService.renderDoc(template, ph, img);
        return attachment(doc, filename);
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
        ph.put("{{ITEM1}}", formatInt(avg.item1Scaled(factor)));
        ph.put("{{ITEM2}}", formatInt(avg.item2Scaled(factor)));
        ph.put("{{ITEM3}}", formatInt(avg.item3Scaled(factor)));
        ph.put("{{TOTAL}}", format1(fs == null || fs.getFinalDefenseScore() == null ? avg.totalScaled(factor) : fs.getFinalDefenseScore()));
        ph.put("{{COMMENT}}", generateComment("PAPER_PROMPT", stu, avg.totalScaled(factor), factor));
    }

    private void fillDesignScores(Map<String, String> ph, List<TeacherScoreRecord> records, double factor, StudentFinalScore fs, Student stu) {
        AvgScores avg = avgDesign(records);
        ph.put("{{ITEM1}}", formatInt(avg.item1Scaled(factor)));
        ph.put("{{ITEM2}}", formatInt(avg.item2Scaled(factor)));
        ph.put("{{ITEM3}}", formatInt(avg.item3Scaled(factor)));
        ph.put("{{ITEM4}}", formatInt(avg.item4Scaled(factor)));
        ph.put("{{ITEM5}}", formatInt(avg.item5Scaled(factor)));
        ph.put("{{ITEM6}}", formatInt(avg.item6Scaled(factor)));
        ph.put("{{TOTAL}}", format1(fs == null || fs.getFinalDefenseScore() == null ? avg.totalScaled(factor) : fs.getFinalDefenseScore()));
        ph.put("{{COMMENT}}", generateComment("DESIGN_PROMPT", stu, avg.totalScaled(factor), factor));
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
            }
        }
        
        // 评阅教师签名
        if (stu.getReviewerTeacherId() != null) {
            byte[] bytes = loadSignature("teacher_" + stu.getReviewerTeacherId());
            if (bytes != null) {
                img.put("{{SIGN_REVIEWER}}", bytes);
                img.put("{{SIGN_REVIEWER_TEACHER}}", bytes); // 兼容性占位符
            }
        }
        
        // 答辩组长签名（用于答辩成绩表）
        if (stu.getDefenseGroupId() != null) {
            try {
                DefenseGroupTeacher leader = defenseGroupTeacherMapper.findLeaderByGroupId(stu.getDefenseGroupId());
                if (leader != null && leader.getTeacherId() != null) {
                    byte[] bytes = loadSignature("teacher_" + leader.getTeacherId());
                    if (bytes != null) {
                        img.put("{{SIGN_LEADER}}", bytes);
                        img.put("{{SIGN_GROUP_LEADER}}", bytes); // 兼容性占位符
                    }
                }
            } catch (Exception ignored) {}
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
        for (String ext : exts) {
            java.nio.file.Path p = basePath.resolve("signatures").resolve(namePrefix + "." + ext);
            if (Files.exists(p)) {
                try {
                    return Files.readAllBytes(p);
                } catch (Exception ignored) {}
            }
        }
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

