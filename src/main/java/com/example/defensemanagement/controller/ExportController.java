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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
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

    @Value("${app.upload.base-dir:uploads}")
    private String uploadBaseDir;

    @GetMapping("/score/paper/{studentId}")
    public ResponseEntity<byte[]> exportPaperScore(@PathVariable Long studentId) {
        return buildScoreDoc(studentId, true);
    }

    @GetMapping("/score/design/{studentId}")
    public ResponseEntity<byte[]> exportDesignScore(@PathVariable Long studentId) {
        return buildScoreDoc(studentId, false);
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
        Student stu = studentMapper.findById(studentId);
        if (stu == null) throw new RuntimeException("学生不存在");
        Integer year = stu.getDefenseYear();
        List<TeacherScoreRecord> records = teacherScoreRecordMapper.findByStudentIdAndYear(studentId, year);
        StudentFinalScore fs = studentFinalScoreMapper.findByStudentIdAndYear(studentId, year);
        double factor = fs != null && fs.getAdjustmentFactor() != null ? fs.getAdjustmentFactor() : 1.0;
        Map<String, String> ph = buildCommonPlaceholders(stu, getDateParts("DEFENSE_DATE"), isPaper);
        Map<String, byte[]> img = new HashMap<>();

        fillSignatures(img, stu);

        if (isPaper) {
            fillPaperScores(ph, records, factor, fs, stu);
            String filename = encode("本科毕业论文答辩成绩表-" + stu.getName() + ".docx");
            byte[] doc = docTemplateService.renderDoc(resolveTemplate("paper-score", PAPER_SCORE_TEMPLATE), ph, img);
            return attachment(doc, filename);
        } else {
            fillDesignScores(ph, records, factor, fs, stu);
            String filename = encode("本科毕业设计答辩成绩表-" + stu.getName() + ".docx");
            byte[] doc = docTemplateService.renderDoc(resolveTemplate("design-score", DESIGN_SCORE_TEMPLATE), ph, img);
            return attachment(doc, filename);
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

        fillSignatures(img, stu);
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
        fillSignatures(img, stu);
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

    private void fillSignatures(Map<String, byte[]> img, Student stu) {
        if (stu.getAdvisorTeacherId() != null) {
            byte[] bytes = loadSignature("teacher_" + stu.getAdvisorTeacherId());
            if (bytes != null) img.put("{{SIGN_ADVISOR}}", bytes);
        }
        if (stu.getReviewerTeacherId() != null) {
            byte[] bytes = loadSignature("teacher_" + stu.getReviewerTeacherId());
            if (bytes != null) img.put("{{SIGN_REVIEWER}}", bytes);
        }
        if (stu.getDefenseGroupId() != null) {
            // Leader signature: use assigned group leader teacher's signature (teacher_{id}.png/jpg)
            try {
                DefenseGroupTeacher leader = defenseGroupTeacherMapper.findLeaderByGroupId(stu.getDefenseGroupId());
                if (leader != null && leader.getTeacherId() != null) {
                    byte[] bytes = loadSignature("teacher_" + leader.getTeacherId());
                    if (bytes != null) img.put("{{SIGN_LEADER}}", bytes);
                }
            } catch (Exception ignored) {}
        }
    }

    private byte[] loadSignature(String namePrefix) {
        String[] exts = {"png", "jpg", "jpeg"};
        for (String ext : exts) {
            java.nio.file.Path p = Paths.get(uploadBaseDir, "signatures", namePrefix + "." + ext);
            if (Files.exists(p)) {
                try {
                    return Files.readAllBytes(p);
                } catch (Exception ignored) {}
            }
        }
        return null;
    }

    private String resolveTemplate(String key, String defaultClasspath) {
        java.nio.file.Path p = Paths.get(uploadBaseDir, "templates", key + ".docx");
        if (Files.exists(p)) {
            return p.toString();
        }
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

