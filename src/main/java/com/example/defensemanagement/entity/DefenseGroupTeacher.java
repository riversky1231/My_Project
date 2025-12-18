package com.example.defensemanagement.entity;

import lombok.Data;

@Data
public class DefenseGroupTeacher {
    private Long id;
    private Long groupId;
    private Long teacherId;
    /**
     * 是否为该组组长（同一组最多1个）
     */
    private Integer isLeader; // 1 leader, 0 member

    // Optional joined info
    private Teacher teacher;
}


