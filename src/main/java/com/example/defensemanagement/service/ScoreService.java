package com.example.defensemanagement.service;

import com.example.defensemanagement.entity.Student;
import com.example.defensemanagement.entity.TeacherScoreRecord;
import com.example.defensemanagement.entity.LargeGroupScore;
import java.util.List;
import java.util.Map;

public interface ScoreService {

    /**
     * 保存/更新教师对某学生的打分记录（同一学生-教师-年份唯一）。
     */
    void saveTeacherScore(TeacherScoreRecord record);

    /**
     * 设置/更新指导教师成绩或评阅人成绩（整数）。
     */
    void setAdvisorScore(Long studentId, Integer year, Integer score);

    void setReviewerScore(Long studentId, Integer year, Integer score);

    /**
     * 设计类：按权值从总分自动拆分 6 个分项并保存教师打分。
     */
    void autoSplitDesignScore(Long studentId, Long teacherId, Integer year, Integer totalScore, Long defenseGroupId);

    /**
     * 计算并落地某答辩小组的平均分、调节系数、最终答辩成绩和总评成绩。
     * @param defenseGroupId 小组ID
     * @param year 答辩年份
     * @param largeGroupScore 优答辩得分（小组第一名在优答辩中的得分），可空；空时调节系数=1
     */
    void finalizeGroupScores(Long defenseGroupId, Integer year, Integer largeGroupScore);
    
    /**
     * 获取小组的调节系数
     */
    Map<String, Object> getGroupAdjustmentFactor(Long groupId, Integer year);

    /**
     * 获取教师所在小组的学生列表（含打分状态）
     */
    Map<String, Object> getTeacherGroupStudents(Long teacherId, Integer year);

    /**
     * 超级管理员：获取所有小组的所有学生（含打分状态）
     */
    Map<String, Object> getAllGroupStudentsForSuperAdmin(Integer year);

    /**
     * 获取每个小组的最高分学生（大组答辩候选人）
     * @param teacherId 当前教师ID，用于查询该教师对每个候选人的打分
     */
    List<Map<String, Object>> getLargeGroupCandidates(Integer year, Long teacherId);

    /**
     * 保存大组答辩打分
     */
    void saveLargeGroupScore(Long studentId, Long teacherId, Integer year, Integer score);

    /**
     * 获取大组答辩学生的所有教师打分及平均分
     */
    Map<String, Object> getLargeGroupStudentScores(Long studentId, Integer year);

    /**
     * 计算某学生小组内的答辩平均分
     */
    Double calculateGroupAvgScore(Long studentId, Integer year);
}

