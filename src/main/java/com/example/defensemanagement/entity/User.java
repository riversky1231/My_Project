package com.example.defensemanagement.entity;

import lombok.Data;

@Data
public class User {
    private Long id;
    private String username;
    private String password;
    private String realName;
    private String email;
    private String phone;
    private Integer status; // 1-启用，0-禁用
    private Long roleId;
    private Long departmentId;
    
    // 关联对象
    private Role role;
    private Department department;
}