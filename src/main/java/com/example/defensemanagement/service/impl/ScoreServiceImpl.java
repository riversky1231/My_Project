package com.example.defensemanagement.service.impl;

import com.example.defensemanagement.entity.Student;
import com.example.defensemanagement.entity.StudentFinalScore;
import com.example.defensemanagement.entity.TeacherScoreRecord;
import com.example.defensemanagement.entity.EvaluationItem;
import com.example.defensemanagement.mapper.StudentFinalScoreMapper;
import com.example.defensemanagement.mapper.StudentMapper;
import com.example.defensemanagement.mapper.TeacherScoreRecordMapper;
import com.example.defensemanagement.service.ConfigService;
import com.example.defensemanagement.service.ScoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.Comparator;
import java.util.HashMap;

@Service
public class ScoreServiceImpl implements ScoreService {

    @Autowired
    private TeacherScoreRecordMapper teacherScoreRecordMapper;

    @Autowired
    private StudentFinalScoreMapper studentFinalScoreMapper;

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private ConfigService configService;

    @Override
    @Transactional
    public void setAdvisorScore(Long studentId, Integer year, Integer score) {
        upsertFinalScore(studentId, year, true, score);
    }

    @Override
    @Transactional
    public void setReviewerScore(Long studentId, Integer year, Integer score) {
        upsertFinalScore(studentId, year, false, score);
    }

    @Override
    @Transactional
    public void saveTeacherScore(TeacherScoreRecord record) {
        if (record.getStudentId() == null || record.getTeacherId() == null || record.getYear() == null) {
            throw new IllegalArgumentException("studentId / teacherId / year 不能为空");
        }
        record.setSubmitTime(LocalDateTime.now());
        TeacherScoreRecord existing = teacherScoreRecordMapper
                .findByStudentIdAndTeacherIdAndYear(record.getStudentId(), record.getTeacherId(), record.getYear());
        if (existing == null) {
            teacherScoreRecordMapper.insert(record);
        } else {
            record.setId(existing.getId());
            teacherScoreRecordMapper.update(record);
        }
    }

    @Override
    @Transactional
    public void autoSplitDesignScore(Long studentId, Long teacherId, Integer year, Integer totalScore, Long defenseGroupId) {
        if (studentId == null || teacherId == null || year == null || totalScore == null) {
            throw new IllegalArgumentException("studentId/teacherId/year/totalScore 不能为空");
        }
        // 权值与最大分值从配置读取，若缺失则回退默认并归一化
        List<EvaluationItem> itemsCfg = configService.getEvaluationItems("DESIGN");
        if (itemsCfg == null) {
            itemsCfg = java.util.Collections.emptyList();
        } else {
            itemsCfg.sort(Comparator.comparingInt(EvaluationItem::getDisplayOrder));
        }
        double[] weights = new double[6];
        int[] maxScores = new int[6];
        if (itemsCfg.size() >= 6) {
            double sum = 0;
            for (int i = 0; i < 6; i++) {
                EvaluationItem it = itemsCfg.get(i);
                double w = it.getWeight() == null ? 0 : it.getWeight();
                weights[i] = w;
                maxScores[i] = it.getMaxScore() == null ? 0 : it.getMaxScore();
                sum += w;
            }
            if (sum == 0) {
                // 防止除零，回退默认
                weights = new double[]{0.15, 0.15, 0.15, 0.25, 0.15, 0.15};
                maxScores = new int[]{15, 15, 15, 25, 15, 15};
            } else {
                // 归一化，确保权重之和为 1
                for (int i = 0; i < weights.length; i++) {
                    weights[i] = weights[i] / sum;
                }
            }
        } else {
            weights = new double[]{0.15, 0.15, 0.15, 0.25, 0.15, 0.15};
            maxScores = new int[]{15, 15, 15, 25, 15, 15};
        }
        int[] items = new int[6];

        // 初步分配
        for (int i = 0; i < weights.length; i++) {
            items[i] = (int) Math.floor(totalScore * weights[i]);
            if (items[i] > maxScores[i]) items[i] = maxScores[i];
        }
        // 调整差值，使总和等于 totalScore，优先从余量大的项加/减
        int sum = java.util.Arrays.stream(items).sum();
        int diff = totalScore - sum;
        int idx = 0;
        while (diff != 0 && idx < items.length * 3) { // 简单循环，最多三轮
            int i = idx % items.length;
            if (diff > 0 && items[i] < maxScores[i]) {
                items[i] += 1;
                diff -= 1;
            } else if (diff < 0 && items[i] > 0) {
                items[i] -= 1;
                diff += 1;
            }
            idx++;
        }

        TeacherScoreRecord record = new TeacherScoreRecord();
        record.setStudentId(studentId);
        record.setTeacherId(teacherId);
        record.setYear(year);
        record.setDefenseGroupId(defenseGroupId);
        record.setItem1Score(items[0]);
        record.setItem2Score(items[1]);
        record.setItem3Score(items[2]);
        record.setItem4Score(items[3]);
        record.setItem5Score(items[4]);
        record.setItem6Score(items[5]);
        record.setTotalScore(totalScore);

        saveTeacherScore(record);
    }

    @Override
    @Transactional
    public void finalizeGroupScores(Long defenseGroupId, Integer year, Integer largeGroupScore) {
        if (defenseGroupId == null || year == null) {
            throw new IllegalArgumentException("defenseGroupId / year 不能为空");
        }
        List<Student> students = studentMapper.findByDefenseGroupId(defenseGroupId);
        if (students == null || students.isEmpty()) {
            return;
        }

        // 计算每个学生在小组内的平均总分
        double championAvgScore = 0.0;
        for (Student stu : students) {
            List<TeacherScoreRecord> records = teacherScoreRecordMapper.findByStudentIdAndYear(stu.getId(), year);
            if (records == null || records.isEmpty()) {
                continue;
            }
            DoubleSummaryStatistics stats = records.stream()
                    .filter(r -> r.getTotalScore() != null)
                    .mapToDouble(TeacherScoreRecord::getTotalScore)
                    .summaryStatistics();
            if (stats.getCount() == 0) {
                continue;
            }
            double groupAvg = round(stats.getAverage(), 2);

            // 更新/插入学生最终成绩
            StudentFinalScore finalScore = studentFinalScoreMapper.findByStudentIdAndYear(stu.getId(), year);
            if (finalScore == null) {
                finalScore = new StudentFinalScore();
                finalScore.setStudentId(stu.getId());
                finalScore.setYear(year);
                finalScore.setAdvisorScore(null);
                finalScore.setReviewerScore(null);
                studentFinalScoreMapper.insert(finalScore);
            }
            finalScore.setGroupAvgScore((int) Math.round(groupAvg));
            studentFinalScoreMapper.update(finalScore);

            if (groupAvg > championAvgScore) {
                championAvgScore = groupAvg;
            }
        }

        // 计算调节系数
        double adjustmentFactor = 1.0;
        if (largeGroupScore != null && championAvgScore > 0) {
            adjustmentFactor = round(largeGroupScore / championAvgScore, 3);
        }

        // 应用调节系数并计算最终答辩成绩、总评成绩
        for (Student stu : students) {
            StudentFinalScore finalScore = studentFinalScoreMapper.findByStudentIdAndYear(stu.getId(), year);
            if (finalScore == null || finalScore.getGroupAvgScore() == null) {
                continue;
            }
            double finalDefenseScore = round(finalScore.getGroupAvgScore() * adjustmentFactor, 2);
            finalScore.setAdjustmentFactor(adjustmentFactor);
            finalScore.setLargeGroupScore(largeGroupScore);
            finalScore.setFinalDefenseScore(finalDefenseScore);

            // 如果已有导师/评阅成绩，则计算总评成绩（30%、30%、40%，一位小数）
            if (finalScore.getAdvisorScore() != null && finalScore.getReviewerScore() != null) {
                double totalGrade = finalScore.getAdvisorScore() * 0.3
                        + finalScore.getReviewerScore() * 0.3
                        + finalDefenseScore * 0.4;
                finalScore.setTotalGrade(round(totalGrade, 1));
            }
            studentFinalScoreMapper.update(finalScore);
        }
    }

    @Override
    public Map<String, Object> getGroupAdjustmentFactor(Long groupId, Integer year) {
        Map<String, Object> result = new HashMap<>();
        List<Student> students = studentMapper.findByDefenseGroupId(groupId);
        if (students == null || students.isEmpty()) {
            result.put("adjustmentFactor", 1.0);
            result.put("message", "小组无学生");
            return result;
        }
        
        // 获取第一个学生的最终成绩（所有学生应该有相同的调节系数）
        Student firstStudent = students.get(0);
        StudentFinalScore fs = studentFinalScoreMapper.findByStudentIdAndYear(firstStudent.getId(), year);
        
        if (fs == null || fs.getAdjustmentFactor() == null) {
            result.put("adjustmentFactor", 1.0);
            result.put("message", "尚未计算调节系数，请先完成小组汇总");
            return result;
        }
        
        result.put("adjustmentFactor", fs.getAdjustmentFactor());
        result.put("groupAvgScore", fs.getGroupAvgScore() != null ? fs.getGroupAvgScore() : 0);
        result.put("largeGroupScore", fs.getLargeGroupScore() != null ? fs.getLargeGroupScore() : 0);
        result.put("finalDefenseScore", fs.getFinalDefenseScore() != null ? fs.getFinalDefenseScore() : 0.0);
        return result;
    }

    private double round(double value, int scale) {
        return BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * 插入/更新最终成绩的导师或评阅分，并在已有答辩成绩时同步刷新总评。
     */
    private void upsertFinalScore(Long studentId, Integer year, boolean isAdvisor, Integer score) {
        if (studentId == null || year == null || score == null) {
            throw new IllegalArgumentException("studentId/year/score 不能为空");
        }
        StudentFinalScore finalScore = studentFinalScoreMapper.findByStudentIdAndYear(studentId, year);
        if (finalScore == null) {
            finalScore = new StudentFinalScore();
            finalScore.setStudentId(studentId);
            finalScore.setYear(year);
            studentFinalScoreMapper.insert(finalScore);
        }
        if (isAdvisor) {
            finalScore.setAdvisorScore(score);
        } else {
            finalScore.setReviewerScore(score);
        }

        // 如果已有答辩成绩，刷新总评
        if (finalScore.getFinalDefenseScore() != null
                && finalScore.getAdvisorScore() != null
                && finalScore.getReviewerScore() != null) {
            double totalGrade = finalScore.getAdvisorScore() * 0.3
                    + finalScore.getReviewerScore() * 0.3
                    + finalScore.getFinalDefenseScore() * 0.4;
            finalScore.setTotalGrade(round(totalGrade, 1));
        }
        studentFinalScoreMapper.update(finalScore);
    }
}

