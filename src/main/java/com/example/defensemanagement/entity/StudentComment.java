package com.example.defensemanagement.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class StudentComment {
    private Long id;
    private Long studentId; // 学生ID
    private Integer year; // 答辩年份
    private String content; // 答辩小组评语内容
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

    // 关联对象
    private Student student;
}
