package com.example.defensemanagement.service;

import com.example.defensemanagement.entity.EvaluationItem;
import com.example.defensemanagement.entity.SystemConfig;
import java.util.List;
import java.util.Map;

public interface ConfigService {

    // --- 评分指标管理 ---

    /**
     * 保存或更新某一类型（论文/设计）的所有评分指标及其权值。
     * 
     * @param defenseType 毕业考核类型: PAPER 或 DESIGN
     * @param items       评分指标列表
     */
    void saveEvaluationItems(String defenseType, List<EvaluationItem> items);

    /**
     * 获取某一类型（论文/设计）的评分指标列表。
     * 
     * @param defenseType 毕业考核类型: PAPER 或 DESIGN
     * @return 评分指标列表
     */
    List<EvaluationItem> getEvaluationItems(String defenseType);

    /**
     * 根据类型获取评分指标及其权值映射，方便计算。
     * 
     * @param defenseType 毕业考核类型
     * @return Map<itemName, weight>
     */
    Map<String, Double> getEvaluationWeights(String defenseType);

    // --- 系统配置管理 ---

    /**
     * 根据Key获取配置值
     */
    String getConfigValue(String key);

    /**
     * 保存或更新系统配置
     */
    void saveConfig(String key, String value, String description);

    // --- 答辩年份/日期管理 ---

    /**
     * 获取当前设置的答辩年份
     */
    Integer getCurrentDefenseYear();

    /**
     * 设置当前答辩年份
     */
    void setCurrentDefenseYear(Integer year);

    /**
     * 设置答辩成绩表的年、月、日
     */
    void setDefenseDate(String dateKey, Integer year, Integer month, Integer day);

    /**
     * 获取配置的日期信息 (例如: DEFENSE_DATE_YEAR)
     */
    String getDefenseDatePart(String dateKey);

    // --- 大模型配置 ---

    /**
     * 配置QWEN大语言模型的APIKEY
     */
    void setQwenApiKey(String apiKey);

    /**
     * 配置评语提示词模板
     */
    void setPromptTemplate(String templateKey, String templateContent);

    /**
     * 获取评语提示词模板
     */
    String getPromptTemplate(String templateKey);

    /**
     * 获取所有年份列表（从学生表中提取）
     */
    List<Integer> getAllYears();
}