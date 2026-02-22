package com.example.defensemanagement.entity;

import lombok.Data;

@Data
public class StudentPreference {
    private Long id;
    private Long studentId;
    private Integer year;
    private Long choice1TeacherId;
    private Long choice2TeacherId;
    private Long choice3TeacherId;
    private String file1Path;
    private String file2Path;
    private String file3Path;
    private Integer status;
    private java.sql.Timestamp createdTime;
    private java.sql.Timestamp updatedTime;

    private Student student;
}
