package com.example.defensemanagement.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 大组答辩成绩表
 * 存储每位教师对进入大组答辩学生的评分
 */
@Data
public class LargeGroupScore {
    private Long id;
    private Long studentId; // 学生ID（小组第一名）
    private Long teacherId; // 评分教师ID
    private Integer year;   // 答辩年份
    private Integer score;  // 大组答辩总分（满分100分）
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

    // 关联对象
    private Student student;
    private Teacher teacher;
}
