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
