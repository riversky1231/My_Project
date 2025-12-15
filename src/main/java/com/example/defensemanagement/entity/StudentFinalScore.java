package com.example.defensemanagement.entity;

import lombok.Data;

@Data
public class StudentFinalScore {
    private Long id;
    private Long studentId;
    private Integer year;

    // 三项基础成绩 (整数)
    private Integer advisorScore; // 指导教师评定成绩
    private Integer reviewerScore; // 评阅人评定成绩

    // 答辩相关
    private Double finalDefenseScore; // 最终的答辩成绩 (小数，写入成绩评定表)
    private Double totalGrade; // 总评成绩 (加权和)
    private Double adjustmentFactor; // 调节系数

    // 大组答辩
    private Integer groupAvgScore; // 小组答辩平均成绩（原始整数）
    private Integer largeGroupScore; // 大组答辩成绩（仅小组第一名有）

    // 关联对象
    private Student student;
}