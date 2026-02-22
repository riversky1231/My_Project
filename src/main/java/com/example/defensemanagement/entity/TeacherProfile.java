package com.example.defensemanagement.entity;

import lombok.Data;

@Data
public class TeacherProfile {
    private Long id;
    private Long teacherId;
    private String researchDirection;
    private String enrollmentRequirements;
    private java.sql.Timestamp updatedTime;

    private Teacher teacher;
}
