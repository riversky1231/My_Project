package com.example.defensemanagement.entity;

import lombok.Data;
import java.util.List;

@Data
public class DefenseGroup {
    private Long id;
    private String name;
    private Long departmentId;  // 所属院系ID
    private Department department;  // 所属院系对象
    private int displayOrder;
    private int score;
    private List<Student> members;  // 使用 Student 代替 GroupMember
    private List<Teacher> teachers;  // 小组教师列表
    private Comment comment;
}
