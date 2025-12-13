package com.example.defensemanagement.entity;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ArchiveDetail {
    private Long id;
    private String sessionName;
    private LocalDateTime archiveTime;
    private List<ArchiveGroup> groups;
    
    @Data
    public static class ArchiveGroup {
        private String name;
        private int score;
        private List<String> members;
        private String comment;
    }
}