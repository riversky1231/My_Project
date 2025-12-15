-- 创建数据库
CREATE DATABASE IF NOT EXISTS defense_management DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE defense_management;

-- 创建答辩小组表
CREATE TABLE IF NOT EXISTS defense_group (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL COMMENT '小组名称',
    display_order INT NOT NULL DEFAULT 0 COMMENT '显示顺序',
    score INT NOT NULL DEFAULT 0 COMMENT '小组得分',
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='答辩小组表';

-- 创建小组成员表
CREATE TABLE IF NOT EXISTS group_member (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '成员姓名',
    group_id BIGINT NOT NULL COMMENT '所属小组ID',
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_group_id (group_id),
    FOREIGN KEY (group_id) REFERENCES defense_group(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='小组成员表';

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

-- 插入初始数据
INSERT INTO defense_group (name, display_order, score) VALUES 
('第一小组：AI智能助手项目', 0, 85),
('第二小组：电商平台系统', 1, 92),
('第三小组：在线教育平台', 2, 78),
('第四小组：智慧城市管理', 3, 88);

-- 插入小组成员
INSERT INTO group_member (name, group_id) VALUES 
('张三', 1), ('李四', 1), ('王五', 1),
('赵六', 2), ('孙七', 2), ('周八', 2),
('钱九', 3), ('吴十', 3), ('郑一', 3),
('王二', 4), ('李三', 4), ('张四', 4);

-- 插入初始评语
INSERT INTO comment (content, group_id) VALUES 
('项目创新性强，技术实现较为完善，演示效果良好。', 1),
('系统架构清晰，功能完整，用户体验优秀。', 2);

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

-- 插入角色数据
INSERT INTO role (name, description) VALUES 
('SUPER_ADMIN', '超级管理员'),
('DEPT_ADMIN', '院系管理员'),
('DEFENSE_LEADER', '答辩组长'),
('TEACHER', '教师');

-- 插入默认院系
INSERT INTO department (name, code, description) VALUES 
('计算机科学与技术学院', 'CS', '计算机科学与技术学院'),
('软件学院', 'SE', '软件学院');

-- 插入超级管理员用户
INSERT INTO user (username, password, real_name, role_id) VALUES 
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', '超级管理员', 1);
-- 密码是123456的BCrypt加密结果

-- 插入示例教师数据
INSERT INTO teacher (teacher_no, name, department_id, title, email, password) VALUES 
('T001', '张教授', 1, '教授', 'zhang@example.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi'),
('T002', '李副教授', 1, '副教授', 'li@example.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi'),
('T003', '王讲师', 2, '讲师', 'wang@example.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi');

-- 插入答辩组长数据（2024年）
INSERT INTO defense_leader (teacher_id, year, department_id) VALUES 
(1, 2024, 1),
(3, 2024, 2);

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