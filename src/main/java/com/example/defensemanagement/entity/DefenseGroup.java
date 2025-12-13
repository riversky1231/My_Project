package com.example.defensemanagement.entity;

import lombok.Data;
import java.util.List;

@Data
public class DefenseGroup {
    private Long id;
    private String name;
    private int displayOrder;
    private int score;
    private List<GroupMember> members;
    private Comment comment;
}
