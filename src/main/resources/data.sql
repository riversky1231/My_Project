CREATE DATABASE IF NOT EXISTS defense_management DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE defense_management;
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 创建答辩小组表
CREATE TABLE IF NOT EXISTS defense_group (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL COMMENT '小组名称',
    display_order INT NOT NULL DEFAULT 0 COMMENT '显示顺序',
    score INT NOT NULL DEFAULT 0 COMMENT '小组得分',
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='答辩小组表';

-- 创建评语表
CREATE TABLE IF NOT EXISTS comment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    content TEXT NOT NULL COMMENT '评语内容',
    group_id BIGINT NOT NULL COMMENT '所属小组ID',
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_group_id (group_id),
    FOREIGN KEY (group_id) REFERENCES defense_group(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='小组评语表';

-- 归档表

-- 创建归档会话表
CREATE TABLE IF NOT EXISTS archive_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_name VARCHAR(255) NOT NULL COMMENT '答辩会议名称',
    archive_time TIMESTAMP NOT NULL COMMENT '归档时间',
    group_count INT NOT NULL COMMENT '小组数量',
    avg_score DECIMAL(5,2) NOT NULL COMMENT '平均分',
    max_score INT NOT NULL COMMENT '最高分',
    archive_data LONGTEXT NOT NULL COMMENT '归档数据(JSON格式)',
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='归档会话表';

-- 创建院系表
CREATE TABLE IF NOT EXISTS department (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '院系名称',
    code VARCHAR(50) NOT NULL UNIQUE COMMENT '院系代码',
    description TEXT COMMENT '院系描述',
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='院系表';

-- 创建角色表
CREATE TABLE IF NOT EXISTS role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE COMMENT '角色名称',
    description VARCHAR(255) COMMENT '角色描述',
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- 创建用户表
CREATE TABLE IF NOT EXISTS user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码(加密)',
    real_name VARCHAR(100) COMMENT '真实姓名',
    email VARCHAR(100) COMMENT '邮箱',
    phone VARCHAR(20) COMMENT '手机号',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    department_id BIGINT COMMENT '院系ID',
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_username (username),
    INDEX idx_role_id (role_id),
    INDEX idx_department_id (department_id),
    FOREIGN KEY (role_id) REFERENCES role(id),
    FOREIGN KEY (department_id) REFERENCES department(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 创建教师表
CREATE TABLE IF NOT EXISTS teacher (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    teacher_no VARCHAR(50) NOT NULL UNIQUE COMMENT '教师编号',
    name VARCHAR(100) NOT NULL COMMENT '教师姓名',
    department_id BIGINT NOT NULL COMMENT '所属院系ID',
    title VARCHAR(50) COMMENT '职称',
    email VARCHAR(100) COMMENT '邮箱',
    phone VARCHAR(20) COMMENT '手机号',
    password VARCHAR(255) COMMENT '登录密码(加密)',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_teacher_no (teacher_no),
    INDEX idx_department_id (department_id),
    FOREIGN KEY (department_id) REFERENCES department(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教师表';

-- 创建答辩组长表
CREATE TABLE IF NOT EXISTS defense_leader (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    teacher_id BIGINT NOT NULL COMMENT '教师ID',
    year INT NOT NULL COMMENT '年份',
    department_id BIGINT NOT NULL COMMENT '院系ID',
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_teacher_year (teacher_id, year),
    INDEX idx_department_year (department_id, year),
    FOREIGN KEY (teacher_id) REFERENCES teacher(id),
    FOREIGN KEY (department_id) REFERENCES department(id),
    UNIQUE KEY uk_teacher_year (teacher_id, year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='答辩组长表';

-- 学生表
CREATE TABLE IF NOT EXISTS t_student (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_no VARCHAR(50) NOT NULL COMMENT '学号',
    name VARCHAR(100) NOT NULL COMMENT '姓名',
    class_info VARCHAR(100) COMMENT '班级',
    department_id BIGINT COMMENT '所属院系ID',
    advisor_teacher_id BIGINT COMMENT '指导教师ID',
    reviewer_teacher_id BIGINT COMMENT '评阅教师ID',
    defense_type VARCHAR(20) COMMENT '毕业考核类型: PAPER 或 DESIGN',
    title VARCHAR(255) COMMENT '毕业考核题目',
    summary TEXT COMMENT '毕业考核摘要',
    defense_group_id BIGINT COMMENT '答辩小组ID',
    defense_year INT COMMENT '答辩年份',
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_student_no_year (student_no, defense_year),
    INDEX idx_department_year (department_id, defense_year),
    INDEX idx_group (defense_group_id),
    FOREIGN KEY (advisor_teacher_id) REFERENCES teacher(id),
    FOREIGN KEY (reviewer_teacher_id) REFERENCES teacher(id),
    FOREIGN KEY (defense_group_id) REFERENCES defense_group(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生表';

-- 教师打分记录表
CREATE TABLE IF NOT EXISTS teacher_score_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL COMMENT '学生ID',
    defense_group_id BIGINT COMMENT '答辩小组ID',
    teacher_id BIGINT NOT NULL COMMENT '打分教师ID',
    year INT NOT NULL COMMENT '答辩年份',
    item1_score INT COMMENT '分项1',
    item2_score INT COMMENT '分项2',
    item3_score INT COMMENT '分项3',
    item4_score INT COMMENT '分项4',
    item5_score INT COMMENT '分项5',
    item6_score INT COMMENT '分项6',
    total_score INT COMMENT '总分',
    submit_time DATETIME COMMENT '提交时间',
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_student_teacher_year (student_id, teacher_id, year),
    INDEX idx_student (student_id),
    FOREIGN KEY (student_id) REFERENCES t_student(id) ON DELETE CASCADE,
    FOREIGN KEY (teacher_id) REFERENCES teacher(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教师打分记录';

-- 学生最终成绩表
CREATE TABLE IF NOT EXISTS student_final_score (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL COMMENT '学生ID',
    year INT NOT NULL COMMENT '年份',
    advisor_score INT COMMENT '指导教师评定成绩',
    reviewer_score INT COMMENT '评阅人评定成绩',
    final_defense_score DECIMAL(6,2) COMMENT '最终答辩成绩',
    total_grade DECIMAL(6,2) COMMENT '总评成绩',
    adjustment_factor DECIMAL(8,3) COMMENT '调节系数',
    group_avg_score INT COMMENT '小组答辩平均成绩',
    large_group_score INT COMMENT '大组答辩成绩',
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_student_year (student_id, year),
    FOREIGN KEY (student_id) REFERENCES t_student(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生最终成绩';

-- 评分指标配置表
CREATE TABLE IF NOT EXISTS evaluation_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    defense_type VARCHAR(20) NOT NULL COMMENT 'PAPER 或 DESIGN',
    item_name VARCHAR(100) NOT NULL COMMENT '指标名称',
    weight DOUBLE COMMENT '权值 0-1',
    max_score INT COMMENT '最大分值',
    display_order INT COMMENT '显示顺序',
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_type (defense_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评分指标配置';

-- 系统配置表
CREATE TABLE IF NOT EXISTS system_config (
    config_key VARCHAR(100) PRIMARY KEY COMMENT '配置键',
    config_value TEXT COMMENT '配置值',
    description VARCHAR(255) COMMENT '描述',
    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置';

-- 基础数据 ------------------------------------------------------------------

-- 角色
INSERT INTO role (name, description) VALUES
('SUPER_ADMIN', '超级管理员'),
('DEPT_ADMIN', '院系管理员'),
('DEFENSE_LEADER', '答辩组长'),
('TEACHER', '教师');

-- 院系
INSERT INTO department (name, code, description) VALUES
('计算机科学与技术学院', 'CS', '计算机科学与技术学院'),
('软件学院', 'SE', '软件学院'),
('信息与通信工程学院', 'ICE', '信息与通信工程'),
('人工智能学院', 'AI', '人工智能学院');

-- 超级管理员用户（密码 123456）
INSERT INTO user (username, password, real_name, role_id, department_id) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', '超级管理员', 1, NULL);

-- 院系管理员
INSERT INTO user (username, password, real_name, role_id, department_id, email, phone) VALUES
('cs_admin',  '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', '计院管理员', 2, 1, 'cs_admin@example.com', '13800000001'),
('se_admin',  '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', '软院管理员', 2, 2, 'se_admin@example.com', '13800000002'),
('ice_admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', '信通管理员', 2, 3, 'ice_admin@example.com', '13800000003');

-- 教师（密码同编号）
INSERT INTO teacher (teacher_no, name, department_id, title, email, phone, password) VALUES
('T001', '张教授', 1, '教授', 'zhang@example.com', '13900010001', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi'),
('T002', '李副教授', 1, '副教授', 'li@example.com', '13900010002', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi'),
('T003', '王讲师', 2, '讲师', 'wang@example.com', '13900020003', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi'),
('T004', '赵老师', 2, '副教授', 'zhao@example.com', '13900020004', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi'),
('T005', '钱博士', 3, '讲师', 'qian@example.com', '13900030005', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi'),
('T006', '孙博士', 4, '副教授', 'sun@example.com', '13900040006', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi'),
('T007', '周老师', 4, '讲师', 'zhou@example.com', '13900040007', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi');

-- 答辩组长
INSERT INTO defense_leader (teacher_id, year, department_id) VALUES
(1, 2024, 1), (1, 2025, 1),
(3, 2024, 2),
(5, 2024, 3),
(6, 2025, 4);

-- 答辩小组与成员（学生通过 defense_group_id 关联到小组）
INSERT INTO defense_group (name, display_order, score) VALUES
('第一小组', 0, 85),
('第二小组',     1, 92),
('第三小组',     2, 78),
('第四小组',     3, 88),
('第五小组',       4, 90);

INSERT INTO comment (content, group_id) VALUES
('项目创新性强，技术实现完善，演示效果良好。', 1),
('系统架构清晰，功能完整，用户体验优秀。', 2),
('教学交互流畅，内容策划合理，但性能需优化。', 3);

-- 学生数据
INSERT INTO t_student (student_no, name, class_info, department_id, advisor_teacher_id, reviewer_teacher_id, defense_type, title, summary, defense_group_id, defense_year) VALUES
('20210001', '林宇轩', '计科2001', 1, 1, 2, 'PAPER', '多模态对话系统研究', '针对多模态对话的模型融合方案。', 1, 2024),
('20210002', '周雨桐', '计科2002', 1, 2, 1, 'DESIGN', '智能助老 APP 设计', '便捷健康监测与陪伴。', 1, 2024),
('20210003', '陈思琪', '软工2001', 2, 3, 4, 'PAPER', '微服务网关安全加固', '零信任与熔断策略结合。', 2, 2024),
('20210004', '王俊凯', '软工2001', 2, 4, 3, 'DESIGN', '校园二手交易平台', '交易、信誉与物流一体化。', 2, 2024),
('20210005', '李怡然', '信通2001', 3, 5, 1, 'PAPER', '5G 上行调度优化', '毫米波场景资源分配。', 3, 2024),
('20210006', '赵梓涵', '信通2002', 3, 5, 2, 'DESIGN', '低功耗传感网关', '面向智慧城市的边缘采集。', 4, 2024),
('20210007', '吴昊', 'AI2101', 4, 6, 7, 'PAPER', '大模型蒸馏与压缩', '蒸馏策略与结构化剪枝。', 5, 2025),
('20210008', '张可欣', 'AI2102', 4, 7, 6, 'DESIGN', '视觉导航小车', '目标检测与路径规划集成。', 5, 2025);

-- 评分指标（权值合计 1.0）
INSERT INTO evaluation_item (defense_type, item_name, weight, max_score, display_order) VALUES
('PAPER',  '论文质量',        0.5, 50, 1),
('PAPER',  '自述报告',        0.25, 25, 2),
('PAPER',  '回答问题',        0.25, 25, 3),
('DESIGN', '设计质量1',       0.15, 15, 1),
('DESIGN', '设计质量2',       0.15, 15, 2),
('DESIGN', '设计质量3',       0.15, 15, 3),
('DESIGN', '自述报告',        0.25, 25, 4),
('DESIGN', '回答问题1',       0.15, 15, 5),
('DESIGN', '回答问题2',       0.15, 15, 6);

-- 教师评分记录
INSERT INTO teacher_score_record (student_id, defense_group_id, teacher_id, year, item1_score, item2_score, item3_score, total_score, submit_time) VALUES
(1, 1, 1, 2024, 45, 23, 22, 90, NOW()),
(1, 1, 2, 2024, 44, 22, 21, 87, NOW()),
(2, 1, 1, 2024, 13, 13, 12, 80, NOW()),
(2, 1, 2, 2024, 14, 14, 13, 85, NOW()),
(3, 2, 3, 2024, 46, 24, 23, 93, NOW()),
(4, 2, 4, 2024, 13, 14, 13, 82, NOW()),
(5, 3, 5, 2024, 41, 21, 20, 82, NOW()),
(6, 4, 5, 2024, 12, 12, 11, 76, NOW()),
(7, 5, 6, 2025, 47, 24, 24, 95, NOW()),
(8, 5, 7, 2025, 13, 14, 14, 85, NOW());

-- 学生最终成绩示例
INSERT INTO student_final_score (student_id, year, advisor_score, reviewer_score, final_defense_score, total_grade, adjustment_factor, group_avg_score, large_group_score) VALUES
(1, 2024, 90, 92, 91.0, 91.6, 1.050, 88, 92),
(2, 2024, 88, 86, 83.0, 85.6, 0.980, 85, 83),
(3, 2024, 92, 90, 93.0, 92.0, 1.020, 91, 93),
(4, 2024, 85, 84, 82.0, 83.4, 0.970, 82, 80),
(5, 2024, 90, 90, 82.0, 86.0, 1.000, 82, NULL),
(6, 2024, 84, 82, 78.0, 80.8, 0.950, 82, 78),
(7, 2025, 95, 94, 96.0, 95.6, 1.030, 93, 96),
(8, 2025, 90, 89, 85.0, 87.2, 0.980, 87, 85);

-- 系统配置示例
INSERT INTO system_config (config_key, config_value, description) VALUES
('CURRENT_DEFENSE_YEAR', '2025', '当前答辩年份'),
('DEFENSE_DATE_YEAR', '2025', '答辩日期-年'),
('DEFENSE_DATE_MONTH', '6', '答辩日期-月'),
('DEFENSE_DATE_DAY', '20', '答辩日期-日'),
('GRADE_DATE_YEAR', '2025', '成绩评定日期-年'),
('GRADE_DATE_MONTH', '6', '成绩评定日期-月'),
('GRADE_DATE_DAY', '28', '成绩评定日期-日'),
('QWEN_API_KEY', 'PLEASE_SET_REAL_KEY', 'QWEN 大模型 API Key'),
('PAPER_PROMPT_TEMPLATE', '请根据论文题目、摘要与答辩表现生成简洁有力的评语。', '论文评语提示词'),
('DESIGN_PROMPT_TEMPLATE', '请基于设计方案、实现细节与现场表现生成客观评语。', '设计评语提示词');

SET FOREIGN_KEY_CHECKS = 1;