package com.example.defensemanagement.service;

import com.example.defensemanagement.entity.TeacherScoreRecord;

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
}

