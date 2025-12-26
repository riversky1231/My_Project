CREATE DATABASE IF NOT EXISTS defense_management DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE defense_management;
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for comment
-- ----------------------------
DROP TABLE IF EXISTS `comment`;
CREATE TABLE `comment`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '评语内容',
  `group_id` bigint NOT NULL COMMENT '所属小组ID',
  `created_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_group_id`(`group_id` ASC) USING BTREE,
  CONSTRAINT `comment_ibfk_1` FOREIGN KEY (`group_id`) REFERENCES `defense_group` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '小组评语表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of comment
-- ----------------------------
INSERT INTO `comment` VALUES (1, '项目创新性强，技术实现完善，演示效果良好。', 1, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `comment` VALUES (2, '系统架构清晰，功能完整，用户体验优秀。', 2, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `comment` VALUES (3, '教学交互流畅，内容策划合理，但性能需优化。', 3, '2025-12-18 12:38:58', '2025-12-18 12:38:58');

-- ----------------------------
-- Table structure for defense_group
-- ----------------------------
DROP TABLE IF EXISTS `defense_group`;
CREATE TABLE `defense_group`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '小组名称',
  `display_order` int NOT NULL DEFAULT 0 COMMENT '显示顺序',
  `score` int NOT NULL DEFAULT 0 COMMENT '小组得分',
  `created_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '答辩小组表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of defense_group
-- ----------------------------
INSERT INTO `defense_group` VALUES (1, '第一小组', 0, 85, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `defense_group` VALUES (2, '第二小组', 1, 92, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `defense_group` VALUES (3, '第三小组', 2, 78, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `defense_group` VALUES (4, '第四小组', 3, 88, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `defense_group` VALUES (5, '第五小组', 4, 90, '2025-12-18 12:38:58', '2025-12-18 12:38:58');

-- ----------------------------
-- Table structure for defense_group_teacher
-- ----------------------------
DROP TABLE IF EXISTS `defense_group_teacher`;
CREATE TABLE `defense_group_teacher`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `group_id` bigint NOT NULL COMMENT '小组ID',
  `teacher_id` bigint NOT NULL COMMENT '教师ID',
  `is_leader` tinyint NOT NULL DEFAULT 0 COMMENT '是否组长：1-是，0-否',
  `created_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_group_teacher`(`group_id` ASC, `teacher_id` ASC) USING BTREE,
  INDEX `idx_group_leader`(`group_id` ASC, `is_leader` ASC) USING BTREE,
  INDEX `teacher_id`(`teacher_id` ASC) USING BTREE,
  CONSTRAINT `defense_group_teacher_ibfk_1` FOREIGN KEY (`group_id`) REFERENCES `defense_group` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `defense_group_teacher_ibfk_2` FOREIGN KEY (`teacher_id`) REFERENCES `teacher` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 30 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '答辩小组教师关联表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of defense_group_teacher
-- ----------------------------
INSERT INTO `defense_group_teacher` VALUES (1, 1, 1, 0, '2025-12-18 12:38:58');
INSERT INTO `defense_group_teacher` VALUES (2, 1, 2, 1, '2025-12-18 12:38:58');
INSERT INTO `defense_group_teacher` VALUES (3, 2, 3, 1, '2025-12-18 12:38:58');
INSERT INTO `defense_group_teacher` VALUES (4, 2, 4, 0, '2025-12-18 12:38:58');
INSERT INTO `defense_group_teacher` VALUES (5, 3, 5, 1, '2025-12-18 12:38:58');
INSERT INTO `defense_group_teacher` VALUES (6, 4, 6, 1, '2025-12-18 12:38:58');
INSERT INTO `defense_group_teacher` VALUES (7, 5, 7, 1, '2025-12-18 12:38:58');
INSERT INTO `defense_group_teacher` VALUES (21, 4, 11, 0, '2025-12-24 21:26:35');
INSERT INTO `defense_group_teacher` VALUES (22, 5, 10, 0, '2025-12-24 21:26:42');
INSERT INTO `defense_group_teacher` VALUES (23, 5, 9, 0, '2025-12-24 21:26:47');
INSERT INTO `defense_group_teacher` VALUES (24, 3, 8, 0, '2025-12-24 21:28:27');

-- ----------------------------
-- Table structure for defense_leader
-- ----------------------------
DROP TABLE IF EXISTS `defense_leader`;
CREATE TABLE `defense_leader`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `teacher_id` bigint NOT NULL COMMENT '教师ID',
  `year` int NOT NULL COMMENT '年份',
  `department_id` bigint NOT NULL COMMENT '院系ID',
  `created_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_teacher_year`(`teacher_id` ASC, `year` ASC) USING BTREE,
  INDEX `idx_teacher_year`(`teacher_id` ASC, `year` ASC) USING BTREE,
  INDEX `idx_department_year`(`department_id` ASC, `year` ASC) USING BTREE,
  CONSTRAINT `defense_leader_ibfk_1` FOREIGN KEY (`teacher_id`) REFERENCES `teacher` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `defense_leader_ibfk_2` FOREIGN KEY (`department_id`) REFERENCES `department` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '答辩组长表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of defense_leader
-- ----------------------------
INSERT INTO `defense_leader` VALUES (1, 1, 2024, 1, '2025-12-18 12:38:58');
INSERT INTO `defense_leader` VALUES (2, 1, 2025, 1, '2025-12-18 12:38:58');
INSERT INTO `defense_leader` VALUES (3, 3, 2024, 2, '2025-12-18 12:38:58');
INSERT INTO `defense_leader` VALUES (4, 5, 2024, 3, '2025-12-18 12:38:58');
INSERT INTO `defense_leader` VALUES (5, 6, 2025, 4, '2025-12-18 12:38:58');
INSERT INTO `defense_leader` VALUES (6, 7, 2024, 4, '2025-12-18 12:38:58');
INSERT INTO `defense_leader` VALUES (7, 8, 2025, 1, '2025-12-18 12:38:58');

-- ----------------------------
-- Table structure for department
-- ----------------------------
DROP TABLE IF EXISTS `department`;
CREATE TABLE `department`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '院系名称',
  `code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '院系代码',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '院系描述',
  `created_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `code`(`code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '院系表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of department
-- ----------------------------
INSERT INTO `department` VALUES (1, '计算机科学与技术学院', 'CS', '计算机科学与技术学院', '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `department` VALUES (2, '软件学院', 'SE', '软件学院', '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `department` VALUES (3, '信息与通信工程学院', 'ICE', '信息与通信工程', '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `department` VALUES (4, '人工智能学院', 'AI', '人工智能学院', '2025-12-18 12:38:58', '2025-12-18 19:49:01');
INSERT INTO `department` VALUES (5, '网络空间安全学院', 'NSC', '网络空间安全学院', '2025-12-18 12:38:58', '2025-12-18 12:38:58');

-- ----------------------------
-- Table structure for evaluation_item
-- ----------------------------
DROP TABLE IF EXISTS `evaluation_item`;
CREATE TABLE `evaluation_item`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `defense_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'PAPER 或 DESIGN',
  `item_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '指标名称',
  `weight` double NULL DEFAULT NULL COMMENT '权值 0-1',
  `max_score` int NULL DEFAULT NULL COMMENT '最大分值',
  `display_order` int NULL DEFAULT NULL COMMENT '显示顺序',
  `created_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_type`(`defense_type` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '评分指标配置' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of evaluation_item
-- ----------------------------
INSERT INTO `evaluation_item` VALUES (1, 'PAPER', '论文质量', 0.5, 50, 1, '2025-12-18 12:38:58');
INSERT INTO `evaluation_item` VALUES (2, 'PAPER', '自述报告', 0.25, 25, 2, '2025-12-18 12:38:58');
INSERT INTO `evaluation_item` VALUES (3, 'PAPER', '回答问题', 0.25, 25, 3, '2025-12-18 12:38:58');
INSERT INTO `evaluation_item` VALUES (4, 'DESIGN', '设计质量1', 0.15, 15, 1, '2025-12-18 12:38:58');
INSERT INTO `evaluation_item` VALUES (5, 'DESIGN', '设计质量2', 0.15, 15, 2, '2025-12-18 12:38:58');
INSERT INTO `evaluation_item` VALUES (6, 'DESIGN', '设计质量3', 0.15, 15, 3, '2025-12-18 12:38:58');
INSERT INTO `evaluation_item` VALUES (7, 'DESIGN', '自述报告', 0.25, 25, 4, '2025-12-18 12:38:58');
INSERT INTO `evaluation_item` VALUES (8, 'DESIGN', '回答问题1', 0.15, 15, 5, '2025-12-18 12:38:58');
INSERT INTO `evaluation_item` VALUES (9, 'DESIGN', '回答问题2', 0.15, 15, 6, '2025-12-18 12:38:58');

-- ----------------------------
-- Table structure for large_group_score
-- ----------------------------
DROP TABLE IF EXISTS `large_group_score`;
CREATE TABLE `large_group_score`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `student_id` bigint NOT NULL COMMENT '学生ID（小组第一名）',
  `teacher_id` bigint NOT NULL COMMENT '评分教师ID',
  `year` int NOT NULL COMMENT '答辩年份',
  `score` int NOT NULL COMMENT '大组答辩总分（满分100分）',
  `created_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_student_teacher_year`(`student_id` ASC, `teacher_id` ASC, `year` ASC) USING BTREE,
  INDEX `idx_student_year`(`student_id` ASC, `year` ASC) USING BTREE,
  INDEX `idx_teacher_year`(`teacher_id` ASC, `year` ASC) USING BTREE,
  CONSTRAINT `large_group_score_ibfk_1` FOREIGN KEY (`student_id`) REFERENCES `t_student` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `large_group_score_ibfk_2` FOREIGN KEY (`teacher_id`) REFERENCES `teacher` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 11 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '大组答辩成绩表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of large_group_score
-- ----------------------------
INSERT INTO `large_group_score` VALUES (1, 2, 1, 2024, 83, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `large_group_score` VALUES (2, 3, 2, 2024, 93, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `large_group_score` VALUES (3, 7, 3, 2025, 96, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `large_group_score` VALUES (4, 9, 1, 2024, 88, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `large_group_score` VALUES (5, 11, 2, 2024, 90, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `large_group_score` VALUES (6, 3, 1, 2024, 93, '2025-12-24 18:32:12', '2025-12-24 18:32:12');
INSERT INTO `large_group_score` VALUES (7, 11, 1, 2024, 94, '2025-12-24 18:32:18', '2025-12-24 18:32:18');
INSERT INTO `large_group_score` VALUES (8, 6, 1, 2024, 93, '2025-12-24 18:32:25', '2025-12-24 18:32:25');
INSERT INTO `large_group_score` VALUES (9, 9, 2, 2024, 99, '2025-12-24 21:37:15', '2025-12-24 21:37:15');
INSERT INTO `large_group_score` VALUES (10, 6, 2, 2024, 93, '2025-12-24 21:37:22', '2025-12-24 21:37:22');

-- ----------------------------
-- Table structure for role
-- ----------------------------
DROP TABLE IF EXISTS `role`;
CREATE TABLE `role`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色名称',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '角色描述',
  `created_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `name`(`name` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '角色表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of role
-- ----------------------------
INSERT INTO `role` VALUES (1, 'SUPER_ADMIN', '超级管理员', '2025-12-18 12:38:58');
INSERT INTO `role` VALUES (2, 'DEPT_ADMIN', '院系管理员', '2025-12-18 12:38:58');
INSERT INTO `role` VALUES (3, 'DEFENSE_LEADER', '答辩组长', '2025-12-18 12:38:58');
INSERT INTO `role` VALUES (4, 'TEACHER', '教师', '2025-12-18 12:38:58');

-- ----------------------------
-- Table structure for student_comment
-- ----------------------------
DROP TABLE IF EXISTS `student_comment`;
CREATE TABLE `student_comment`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `student_id` bigint NOT NULL COMMENT '学生ID',
  `year` int NOT NULL COMMENT '答辩年份',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '答辩小组评语内容',
  `created_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_student_year`(`student_id` ASC, `year` ASC) USING BTREE,
  INDEX `idx_student`(`student_id` ASC) USING BTREE,
  CONSTRAINT `student_comment_ibfk_1` FOREIGN KEY (`student_id`) REFERENCES `t_student` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '学生答辩评语表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of student_comment
-- ----------------------------
INSERT INTO `student_comment` VALUES (1, 2, 2024, '该生设计思路清晰，实现方案合理，演示效果良好，答辩表现优秀。', '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `student_comment` VALUES (2, 3, 2024, '论文选题具有实际意义，研究方法科学，数据分析充分，答辩表达清晰。', '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `student_comment` VALUES (3, 4, 2024, '设计作品功能完整，技术实现到位，用户体验良好，答辩回答问题准确。', '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `student_comment` VALUES (4, 5, 2024, '论文研究深入，理论分析透彻，实验验证充分，答辩表现优秀。', '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `student_comment` VALUES (5, 6, 2024, '设计创新性强，技术方案合理，实现效果良好，答辩回答问题准确。', '2025-12-18 12:38:58', '2025-12-18 12:38:58');

-- ----------------------------
-- Table structure for student_final_score
-- ----------------------------
DROP TABLE IF EXISTS `student_final_score`;
CREATE TABLE `student_final_score`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `student_id` bigint NOT NULL COMMENT '学生ID',
  `year` int NOT NULL COMMENT '年份',
  `advisor_score` int NULL DEFAULT NULL COMMENT '指导教师评定成绩',
  `reviewer_score` int NULL DEFAULT NULL COMMENT '评阅人评定成绩',
  `final_defense_score` decimal(6, 2) NULL DEFAULT NULL COMMENT '最终答辩成绩',
  `total_grade` decimal(6, 2) NULL DEFAULT NULL COMMENT '总评成绩',
  `adjustment_factor` decimal(8, 3) NULL DEFAULT NULL COMMENT '调节系数',
  `group_avg_score` int NULL DEFAULT NULL COMMENT '小组答辩平均成绩',
  `large_group_score` int NULL DEFAULT NULL COMMENT '大组答辩成绩',
  `created_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_student_year`(`student_id` ASC, `year` ASC) USING BTREE,
  CONSTRAINT `student_final_score_ibfk_1` FOREIGN KEY (`student_id`) REFERENCES `t_student` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 12 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '学生最终成绩' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of student_final_score
-- ----------------------------
INSERT INTO `student_final_score` VALUES (2, 2, 2024, 86, 79, 84.70, 83.40, 1.027, 83, 83, '2025-12-18 12:38:58');
INSERT INTO `student_final_score` VALUES (3, 3, 2024, 92, 90, 93.00, 91.80, 0.995, 94, 93, '2025-12-18 12:38:58');
INSERT INTO `student_final_score` VALUES (4, 4, 2024, 85, 84, 74.60, 80.50, 0.995, 75, 80, '2025-12-18 12:38:58');
INSERT INTO `student_final_score` VALUES (5, 5, 2024, 90, 90, 77.00, 84.80, 0.939, 82, NULL, '2025-12-18 12:38:58');
INSERT INTO `student_final_score` VALUES (6, 6, 2024, 84, 82, 93.00, 87.00, 1.057, 88, 93, '2025-12-18 12:38:58');
INSERT INTO `student_final_score` VALUES (7, 7, 2025, 95, 94, 96.00, 95.60, 1.030, 93, 96, '2025-12-18 12:38:58');
INSERT INTO `student_final_score` VALUES (8, 8, 2025, 90, 89, 85.00, 87.20, 0.980, 87, 85, '2025-12-18 12:38:58');
INSERT INTO `student_final_score` VALUES (9, 9, 2024, 87, 85, 93.50, 89.00, 1.027, 91, 94, '2025-12-18 12:38:58');
INSERT INTO `student_final_score` VALUES (10, 10, 2024, 89, 88, 89.60, 88.90, 0.995, 90, 86, '2025-12-18 12:38:58');
INSERT INTO `student_final_score` VALUES (11, 11, 2024, 91, 89, 92.00, 90.80, 0.939, 98, 92, '2025-12-18 12:38:58');

-- ----------------------------
-- Table structure for system_config
-- ----------------------------
DROP TABLE IF EXISTS `system_config`;
CREATE TABLE `system_config`  (
  `config_key` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '配置键',
  `config_value` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '配置值',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '描述',
  `updated_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`config_key`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '系统配置' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of system_config
-- ----------------------------
INSERT INTO `system_config` VALUES ('CURRENT_DEFENSE_YEAR', '2024', '当前答辩年份', '2025-12-23 18:17:31');
INSERT INTO `system_config` VALUES ('DEFENSE_DATE_DAY', '20', '答辩日期-日', '2025-12-18 12:38:58');
INSERT INTO `system_config` VALUES ('DEFENSE_DATE_MONTH', '6', '答辩日期-月', '2025-12-18 12:38:58');
INSERT INTO `system_config` VALUES ('DEFENSE_DATE_YEAR', '2025', '答辩日期-年', '2025-12-18 12:38:58');
INSERT INTO `system_config` VALUES ('DESIGN_PROMPT_TEMPLATE', '请基于设计方案、实现细节与现场表现生成客观评语。', '设计评语提示词', '2025-12-18 12:38:58');
INSERT INTO `system_config` VALUES ('GRADE_DATE_DAY', '28', '成绩评定日期-日', '2025-12-18 12:38:58');
INSERT INTO `system_config` VALUES ('GRADE_DATE_MONTH', '6', '成绩评定日期-月', '2025-12-18 12:38:58');
INSERT INTO `system_config` VALUES ('GRADE_DATE_YEAR', '2025', '成绩评定日期-年', '2025-12-18 12:38:58');
INSERT INTO `system_config` VALUES ('PAPER_PROMPT_TEMPLATE', '请根据论文题目、摘要与答辩表现生成简洁有力的评语。', '论文评语提示词', '2025-12-18 12:38:58');
INSERT INTO `system_config` VALUES ('QWEN_API_KEY', 'sk-115af915697b44df899340c9e39b13f4', 'QWEN 大模型 API Key', '2025-12-23 19:33:55');

-- ----------------------------
-- Table structure for t_student
-- ----------------------------
DROP TABLE IF EXISTS `t_student`;
CREATE TABLE `t_student`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `student_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '学号',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '姓名',
  `class_info` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '班级',
  `department_id` bigint NULL DEFAULT NULL COMMENT '所属院系ID',
  `advisor_teacher_id` bigint NULL DEFAULT NULL COMMENT '指导教师ID',
  `reviewer_teacher_id` bigint NULL DEFAULT NULL COMMENT '评阅教师ID',
  `defense_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '毕业考核类型: PAPER 或 DESIGN',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '毕业考核题目',
  `summary` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '毕业考核摘要',
  `defense_group_id` bigint NULL DEFAULT NULL COMMENT '答辩小组ID',
  `defense_year` int NULL DEFAULT NULL COMMENT '答辩年份',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '学生联系电话',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '学生邮箱',
  `defense_date` date NULL DEFAULT NULL COMMENT '答辩日期',
  `created_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_student_no_year`(`student_no` ASC, `defense_year` ASC) USING BTREE,
  INDEX `idx_department_year`(`department_id` ASC, `defense_year` ASC) USING BTREE,
  INDEX `idx_group`(`defense_group_id` ASC) USING BTREE,
  INDEX `advisor_teacher_id`(`advisor_teacher_id` ASC) USING BTREE,
  INDEX `reviewer_teacher_id`(`reviewer_teacher_id` ASC) USING BTREE,
  CONSTRAINT `t_student_ibfk_1` FOREIGN KEY (`advisor_teacher_id`) REFERENCES `teacher` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `t_student_ibfk_2` FOREIGN KEY (`reviewer_teacher_id`) REFERENCES `teacher` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `t_student_ibfk_3` FOREIGN KEY (`defense_group_id`) REFERENCES `defense_group` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 24 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '学生表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of t_student
-- ----------------------------
INSERT INTO `t_student` VALUES (2, '20210002', '周雨桐', '计科2002', 5, 2, 1, 'DESIGN', '智能助老 APP 设计', '便捷健康监测与陪伴。', 1, 2024, '13800000001', 'zhouyt@example.com', '2025-06-20', '2025-12-18 12:38:58', '2025-12-26 12:20:35');
INSERT INTO `t_student` VALUES (3, '20210003', '陈思琪', '软工2001', 4, 3, 4, 'PAPER', '微服务网关安全加固', '零信任与熔断策略结合。', 2, 2024, '13800000002', 'chensq@example.com', '2025-06-20', '2025-12-18 12:38:58', '2025-12-26 12:20:32');
INSERT INTO `t_student` VALUES (4, '20210004', '王俊凯', '软工2001', 3, 4, 3, 'DESIGN', '校园二手交易平台', '交易、信誉与物流一体化。', 2, 2024, '13800000003', 'wangjk@example.com', '2025-06-20', '2025-12-18 12:38:58', '2025-12-26 12:20:28');
INSERT INTO `t_student` VALUES (5, '20210005', '李怡然', '信通2001', 2, 5, 8, 'PAPER', '5G 上行调度优化', '毫米波场景资源分配。', 3, 2024, '13800000004', 'liyr@example.com', '2025-06-20', '2025-12-18 12:38:58', '2025-12-26 12:20:02');
INSERT INTO `t_student` VALUES (6, '20210006', '赵梓涵', '信通2002', 1, 6, 11, 'DESIGN', '低功耗传感网关', '面向智慧城市的边缘采集。', 4, 2024, '13800000005', 'zhaozh@example.com', '2025-06-20', '2025-12-18 12:38:58', '2025-12-24 21:30:06');
INSERT INTO `t_student` VALUES (7, '20210007', '吴昊', 'AI2101', 4, 6, 7, 'PAPER', '大模型蒸馏与压缩', '蒸馏策略与结构化剪枝。', 5, 2025, '13800000006', 'wuhao@example.com', '2025-06-20', '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `t_student` VALUES (8, '20210008', '张可欣', 'AI2102', 4, 7, 6, 'DESIGN', '视觉导航小车', '目标检测与路径规划集成。', 5, 2025, '13800000007', 'zhangkx@example.com', '2025-06-20', '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `t_student` VALUES (9, '20210009', '刘明', '计科2001', 1, 1, 2, 'PAPER', '区块链共识算法优化', '提升共识效率与安全性。', 1, 2024, '13800000008', 'liuming@example.com', '2025-06-20', '2025-12-18 12:38:58', '2025-12-24 21:25:01');
INSERT INTO `t_student` VALUES (10, '20210010', '陈静', '软工2002', 1, 3, 4, 'DESIGN', '在线教育平台设计', '支持多终端实时互动。', 2, 2024, '13800000009', 'chenjing@example.com', '2025-06-20', '2025-12-18 12:38:58', '2025-12-24 21:25:08');
INSERT INTO `t_student` VALUES (11, '20210011', '杨帆', '信通2003', 1, 8, 5, 'PAPER', '物联网安全协议研究', '轻量级加密与认证机制。', 3, 2024, '13800000010', 'yangfan@example.com', '2025-06-20', '2025-12-18 12:38:58', '2025-12-24 21:29:51');
INSERT INTO `t_student` VALUES (21, '20210021', '张三', NULL, 1, NULL, NULL, 'PAPER', '基于深度学习的图像识别研究', NULL, NULL, 2024, NULL, NULL, NULL, '2025-12-26 18:00:02', '2025-12-26 18:01:43');
INSERT INTO `t_student` VALUES (22, '20210022', '李四', NULL, 1, NULL, NULL, 'DESIGN', '智能家居控制系统设计', NULL, NULL, 2024, NULL, NULL, NULL, '2025-12-26 18:00:02', '2025-12-26 18:01:43');
INSERT INTO `t_student` VALUES (23, '20210023', '王五', NULL, 2, NULL, NULL, 'PAPER', '区块链技术在金融领域的应用', NULL, NULL, 2024, NULL, NULL, NULL, '2025-12-26 18:00:02', '2025-12-26 18:01:43');

-- ----------------------------
-- Table structure for teacher
-- ----------------------------
DROP TABLE IF EXISTS `teacher`;
CREATE TABLE `teacher`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `teacher_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '教师编号',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '教师姓名',
  `department_id` bigint NOT NULL COMMENT '所属院系ID',
  `title` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '职称',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '手机号',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '登录密码(加密)',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
  `user_id` bigint NULL DEFAULT NULL COMMENT '关联的用户ID（user表），用于统一管理',
  `created_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `teacher_no`(`teacher_no` ASC) USING BTREE,
  INDEX `idx_teacher_no`(`teacher_no` ASC) USING BTREE,
  INDEX `idx_department_id`(`department_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  CONSTRAINT `teacher_ibfk_1` FOREIGN KEY (`department_id`) REFERENCES `department` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `teacher_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 19 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '教师表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of teacher
-- ----------------------------
INSERT INTO `teacher` VALUES (1, 'T001', '张教授', 1, '教授', 'zhang@example.com', NULL, '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 1, 5, '2025-12-18 12:38:58', '2025-12-23 15:11:39');
INSERT INTO `teacher` VALUES (2, 'T002', '李副教授', 1, '副教授', 'li@example.com', NULL, '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 1, 6, '2025-12-18 12:38:58', '2025-12-23 15:11:47');
INSERT INTO `teacher` VALUES (3, 'T003', '王讲师', 2, '讲师', 'wang@example.com', '13900020003', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 1, 7, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `teacher` VALUES (4, 'T004', '赵老师', 2, '副教授', 'zhao@example.com', '13900020004', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 1, 8, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `teacher` VALUES (5, 'T005', '钱博士', 3, '讲师', 'qian@example.com', '13900030005', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 1, 9, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `teacher` VALUES (6, 'T006', '孙博士', 4, '副教授', 'sun@example.com', '13900040006', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 1, 10, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `teacher` VALUES (7, 'T007', '郑老师', 4, '讲师', 'zheng@example.com', '13900040007', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 1, 11, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `teacher` VALUES (8, 'T008', '周老师', 1, '讲师', 'zhou@example.com', '13900010008', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 1, 12, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `teacher` VALUES (9, 'T009', '吴老师', 2, '副教授', 'wu@example.com', '13900020009', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 1, 13, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `teacher` VALUES (10, 'T010', '徐老师', 3, '讲师', 'xu@example.com', '13900030010', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 1, NULL, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `teacher` VALUES (11, 'T015', '张老师', 1, '讲师', 'zhang@example.com', NULL, '$2a$10$2LiCF0JzmmIAor5RXQUAZu.QR5tWVJxnn7K0cnUuC.Lk5sSQt3sTq', 1, 15, '2025-12-23 14:53:44', '2025-12-26 16:37:48');
INSERT INTO `teacher` VALUES (12, 'T018', '王五', 1, '讲师', NULL, NULL, '$2a$10$0Q6lycvtu/HXjoDxDbGRve177S4eSdSuHa0QhNnv.dF4y0miYo/sy', 1, NULL, '2025-12-26 16:31:50', '2025-12-26 16:37:53');
INSERT INTO `teacher` VALUES (13, 'T019', '张三', 1, NULL, NULL, NULL, '$2a$10$ZH8KbWWLq9xiZRk8rxES5uhuas.u42Fn4RAouYIklA8JxWc9XewPy', 1, NULL, '2025-12-26 17:00:15', '2025-12-26 17:00:15');
INSERT INTO `teacher` VALUES (14, 'T020', '李四', 1, NULL, NULL, NULL, '$2a$10$0n7jPsHjZdY2Wd1znjZKAeRat67SjUE3Lh9XCyAn/P7BYghmj8PcS', 1, NULL, '2025-12-26 17:00:15', '2025-12-26 17:00:15');
INSERT INTO `teacher` VALUES (15, 'T021', '王五', 1, NULL, NULL, NULL, '$2a$10$YbnYxvvNEPvIXZCJ08yGpuaP66bEkKT6RZLDl6kOWV1Wann5ME59W', 1, NULL, '2025-12-26 17:00:16', '2025-12-26 17:00:16');
INSERT INTO `teacher` VALUES (16, 'T022', '玉小刚', 1, NULL, NULL, NULL, '$2a$10$EUzN3Ehb.61HfH3n/w.9xebiJ7nMkBdXOJW2k6rX/s5Fpz/NEwYtK', 1, NULL, '2025-12-26 18:01:09', '2025-12-26 18:01:09');
INSERT INTO `teacher` VALUES (17, 'T023', '柳二龙', 1, NULL, NULL, NULL, '$2a$10$i2uLlKzJv6fpIVoqGjCg1O0ifh3yqBODMuA54ZS83Qohhjvl5SmdW', 1, NULL, '2025-12-26 18:01:10', '2025-12-26 18:01:10');
INSERT INTO `teacher` VALUES (18, 'T024', '撒大大', 1, NULL, NULL, NULL, '$2a$10$nEyKSDizvrZ25iOm97LXTO1dC29IXpZcHoVnET6eQFpVgIf3YLrDq', 1, NULL, '2025-12-26 18:01:10', '2025-12-26 18:01:10');

-- ----------------------------
-- Table structure for teacher_score_record
-- ----------------------------
DROP TABLE IF EXISTS `teacher_score_record`;
CREATE TABLE `teacher_score_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `student_id` bigint NOT NULL COMMENT '学生ID',
  `defense_group_id` bigint NULL DEFAULT NULL COMMENT '答辩小组ID',
  `teacher_id` bigint NOT NULL COMMENT '打分教师ID',
  `year` int NOT NULL COMMENT '答辩年份',
  `item1_score` int NULL DEFAULT NULL COMMENT '分项1',
  `item2_score` int NULL DEFAULT NULL COMMENT '分项2',
  `item3_score` int NULL DEFAULT NULL COMMENT '分项3',
  `item4_score` int NULL DEFAULT NULL COMMENT '分项4',
  `item5_score` int NULL DEFAULT NULL COMMENT '分项5',
  `item6_score` int NULL DEFAULT NULL COMMENT '分项6',
  `total_score` int NULL DEFAULT NULL COMMENT '总分',
  `submit_time` datetime NULL DEFAULT NULL COMMENT '提交时间',
  `created_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_student_teacher_year`(`student_id` ASC, `teacher_id` ASC, `year` ASC) USING BTREE,
  INDEX `idx_student`(`student_id` ASC) USING BTREE,
  INDEX `teacher_id`(`teacher_id` ASC) USING BTREE,
  CONSTRAINT `teacher_score_record_ibfk_1` FOREIGN KEY (`student_id`) REFERENCES `t_student` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `teacher_score_record_ibfk_2` FOREIGN KEY (`teacher_id`) REFERENCES `teacher` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 22 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '教师打分记录' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of teacher_score_record
-- ----------------------------
INSERT INTO `teacher_score_record` VALUES (3, 2, 1, 1, 2024, 13, 13, 12, 14, 13, 14, 79, '2025-12-24 09:28:43', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (4, 2, 1, 2, 2024, 14, 14, 13, 20, 12, 13, 86, '2025-12-24 17:27:01', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (5, 3, 2, 3, 2024, 46, 24, 23, NULL, NULL, NULL, 93, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (6, 4, 2, 4, 2024, 13, 14, 13, 22, 13, 11, 86, '2025-12-24 18:23:09', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (7, 5, 3, 5, 2024, 41, 21, 20, NULL, NULL, NULL, 82, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (8, 6, 4, 5, 2024, 12, 12, 11, NULL, NULL, NULL, 76, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (9, 7, 5, 6, 2025, 47, 24, 24, NULL, NULL, NULL, 95, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (10, 8, 5, 7, 2025, 13, 14, 14, NULL, NULL, NULL, 85, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `teacher_score_record` VALUES (11, 3, 2, 4, 2024, 47, 24, 23, NULL, NULL, NULL, 94, '2025-12-24 18:22:52', '2025-12-24 18:22:52');
INSERT INTO `teacher_score_record` VALUES (12, 10, 2, 4, 2024, 12, 13, 14, 21, 9, 11, 80, '2025-12-24 18:23:25', '2025-12-24 18:23:24');
INSERT INTO `teacher_score_record` VALUES (13, 4, 2, 3, 2024, 9, 9, 9, 19, 9, 9, 64, '2025-12-24 18:24:15', '2025-12-24 18:24:15');
INSERT INTO `teacher_score_record` VALUES (14, 10, 2, 3, 2024, 15, 15, 15, 25, 15, 15, 100, '2025-12-24 18:24:25', '2025-12-24 18:24:25');
INSERT INTO `teacher_score_record` VALUES (15, 11, 3, 5, 2024, 50, 23, 25, NULL, NULL, NULL, 98, '2025-12-24 18:26:36', '2025-12-24 18:26:36');
INSERT INTO `teacher_score_record` VALUES (16, 6, 4, 6, 2024, 14, 14, 14, 23, 14, 14, 93, '2025-12-24 18:27:15', '2025-12-24 18:27:14');
INSERT INTO `teacher_score_record` VALUES (17, 9, 1, 2, 2024, 46, 21, 21, NULL, NULL, NULL, 88, '2025-12-24 18:30:32', '2025-12-24 18:30:31');
INSERT INTO `teacher_score_record` VALUES (18, 9, 1, 1, 2024, 50, 23, 21, NULL, NULL, NULL, 94, '2025-12-24 18:30:53', '2025-12-24 18:30:53');
INSERT INTO `teacher_score_record` VALUES (19, 5, 3, 8, 2024, 50, 20, 21, NULL, NULL, NULL, 91, '2025-12-24 21:31:53', '2025-12-24 21:31:53');
INSERT INTO `teacher_score_record` VALUES (20, 11, 3, 8, 2024, 48, 22, 23, NULL, NULL, NULL, 93, '2025-12-24 21:32:02', '2025-12-24 21:32:01');
INSERT INTO `teacher_score_record` VALUES (21, 6, 4, 11, 2024, 13, 13, 15, 24, 15, 15, 95, '2025-12-24 21:33:06', '2025-12-24 21:33:06');

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户名',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '密码(加密)',
  `real_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '真实姓名',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '手机号',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `department_id` bigint NULL DEFAULT NULL COMMENT '院系ID',
  `created_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `username`(`username` ASC) USING BTREE,
  INDEX `idx_username`(`username` ASC) USING BTREE,
  INDEX `idx_role_id`(`role_id` ASC) USING BTREE,
  INDEX `idx_department_id`(`department_id` ASC) USING BTREE,
  CONSTRAINT `user_ibfk_1` FOREIGN KEY (`role_id`) REFERENCES `role` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `user_ibfk_2` FOREIGN KEY (`department_id`) REFERENCES `department` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 25 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of user
-- ----------------------------
INSERT INTO `user` VALUES (1, 'admin', '$2a$10$OJU0o3Fw0h03uOyubGypyeX2OlxAb9Zlxfm5CKGRl1ybvrcxnvGky', '超级管理员', '', NULL, 1, 1, NULL, '2025-12-18 12:38:58', '2025-12-23 16:17:31');
INSERT INTO `user` VALUES (2, 'cs_admin', '$2a$10$tVBf3h/9H.XjXnUNDQ85wukXmhJflZrjw7yJo9jP0f9hQt5o1TrbO', '计院管理员', 'cs_admin@example.com', '13800000001', 1, 2, 1, '2025-12-18 12:38:58', '2025-12-23 15:11:53');
INSERT INTO `user` VALUES (3, 'se_admin', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '软院管理员', 'se_admin@example.com', '13800000002', 1, 2, 2, '2025-12-18 12:38:58', '2025-12-18 12:40:03');
INSERT INTO `user` VALUES (4, 'ice_admin', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '信通管理员', 'ice_admin@example.com', '13800000003', 1, 2, 3, '2025-12-18 12:38:58', '2025-12-18 12:40:06');
INSERT INTO `user` VALUES (5, 'T001', '$2a$10$kW60xeovCtfW.8ypPg.tXuRtESt0O2Uyc91JmnR9PdY.RSTxMg.R.', '张教授', 'zhang@example.com', '13900010001', 1, 4, 1, '2025-12-18 12:38:58', '2025-12-23 15:11:39');
INSERT INTO `user` VALUES (6, 'T002', '$2a$10$GuThW0QCkFfr1vgPrcPCmuCUt9fZnIZYuST/4jL6aFXX0K9t7ESci', '李副教授', 'li@example.com', '13900010002', 1, 4, 1, '2025-12-18 12:38:58', '2025-12-23 15:11:47');
INSERT INTO `user` VALUES (7, 'T003', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '王讲师', 'wang@example.com', '13900020003', 1, 4, 2, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `user` VALUES (8, 'T004', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '赵老师', 'zhao@example.com', '13900020004', 1, 4, 2, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `user` VALUES (9, 'T005', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '钱博士', 'qian@example.com', '13900030005', 1, 4, 3, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `user` VALUES (10, 'T006', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '孙博士', 'sun@example.com', '13900040006', 1, 4, 4, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `user` VALUES (11, 'T007', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '郑老师', 'zheng@example.com', '13900040007', 1, 4, 4, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `user` VALUES (12, 'T008', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '周老师', 'zhou@example.com', '13900010008', 1, 4, 1, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `user` VALUES (13, 'T009', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '吴老师', 'wu@example.com', '13900020009', 1, 4, 2, '2025-12-18 12:38:58', '2025-12-18 12:38:58');
INSERT INTO `user` VALUES (15, 'T010', '$2a$10$g2wkN7ssThzXj6iru5WFYuQTbTOKP3ygt1Q96tPqAd6PBISt2Uzba', '张老师', 'zhang@example.com', NULL, 1, 4, 2, '2025-12-23 14:53:44', '2025-12-24 18:28:40');

SET FOREIGN_KEY_CHECKS = 1;
