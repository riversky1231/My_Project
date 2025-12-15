package com.example.defensemanagement.entity;

import lombok.Data;

@Data
public class EvaluationItem {
    private Long id;
    private String defenseType; // PAPER(论文) 或 DESIGN(设计)
    private String itemName; // 评价指标名称 (如: 论文质量, 设计质量1)
    private Double weight; // 评分权值 (0.0 - 1.0)
    private Integer maxScore; // 对应权值的满分值 (权值 * 100)
    private Integer displayOrder; // 显示顺序
}