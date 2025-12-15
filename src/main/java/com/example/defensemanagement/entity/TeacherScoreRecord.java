package com.example.defensemanagement.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TeacherScoreRecord {
    private Long id;
    private Long studentId; // 学生ID
    private Long defenseGroupId; // 答辩小组ID
    private Long teacherId; // 打分教师ID (评委)
    private Integer year; // 答辩年份

    // 分项成绩 (存储JSON或映射到多个字段，这里简化为字段)
    private Integer item1Score; // 论文质量/设计质量1
    private Integer item2Score; // 答辩自述报告/设计质量2
    private Integer item3Score; // 回答问题情况/设计质量3
    private Integer item4Score; // 仅设计类型: 答辩的自述报告成绩
    private Integer item5Score; // 仅设计类型: 回答问题情况分项1
    private Integer item6Score; // 仅设计类型: 回答问题情况分项2

    private Integer totalScore; // 教师给出的总分 (合计成绩)
    private LocalDateTime submitTime;

    // 关联对象
    private Student student;
    private Teacher teacher;
}