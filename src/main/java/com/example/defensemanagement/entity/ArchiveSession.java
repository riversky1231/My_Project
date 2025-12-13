package com.example.defensemanagement.entity;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ArchiveSession {
    private Long id;
    private String sessionName;
    private LocalDateTime archiveTime;
    private int groupCount;
    private double avgScore;
    private int maxScore;
    private String archiveData; // JSON格式存储归档数据
}