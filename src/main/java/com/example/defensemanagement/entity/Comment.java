package com.example.defensemanagement.entity;

import lombok.Data;

@Data
public class Comment {
    private Long id;
    private String content;
    private Long groupId;
}
