package com.example.defensemanagement.entity;

import lombok.Data;
import java.util.List;

@Data
public class DefenseGroup {
    private Long id;
    private String name;
    private int displayOrder;
    private int score;
    private List<Student> members;  // 使用 Student 代替 GroupMember
    private Comment comment;
}
