package com.example.defensemanagement.entity;

import lombok.Data;

@Data
public class DefenseLeader {
    private Long id;
    private Long teacherId;
    private Integer year;
    private Long departmentId;
    
    // 关联对象
    private Teacher teacher;
    private Department department;
}