package com.example.defensemanagement.entity;

import lombok.Data;

@Data
public class GroupMember {
    private Long id;
    private String name;
    private Long groupId;
}
