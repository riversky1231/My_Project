package com.example.defensemanagement.entity;

import lombok.Data;

@Data
public class Teacher {
    private Long id;
    private String teacherNo;
    private String name;
    private Long departmentId;
    private String title;
    private String email;
    private String phone;
    private String password;
    private Integer status; // 1-启用，0-禁用
    private Long userId; // 关联的用户ID（user表）
    
    // 关联对象
    private Department department;
}