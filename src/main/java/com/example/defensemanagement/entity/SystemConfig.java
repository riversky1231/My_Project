package com.example.defensemanagement.entity;

import lombok.Data;

@Data
public class SystemConfig {
    private String configKey; // 键，如: CURRENT_DEFENSE_YEAR, QWEN_API_KEY
    private String configValue; // 值
    private String description;
}