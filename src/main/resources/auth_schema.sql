-- 权限管理相关表结构

-- 1. 用户表
CREATE TABLE sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名（登录账号）',
    password VARCHAR(100) NOT NULL COMMENT '密码',
    real_name VARCHAR(50) NOT NULL COMMENT '真实姓名',
    teacher_code VARCHAR(20) COMMENT '教师编号（教师角色使用）',
    email VARCHAR(100) COMMENT '邮箱',
    phone VARCHAR(20) COMMENT '手机号',
    status TINYINT DEFAULT 1 COMMENT '用户状态：1-启用，0-禁用',
    department_id BIGINT COMMENT '所属部门ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by BIGINT COMMENT '创建人ID',
    update_by BIGINT COMMENT '更新人ID',
    INDEX idx_username (username),
    INDEX idx_teacher_code (teacher_code),
    INDEX idx_department_id (department_id)
) COMMENT='用户表';

-- 2. 角色表
CREATE TABLE sys_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '角色ID',
    role_code VARCHAR(50) NOT NULL UNIQUE COMMENT '角色编码',
    role_name VARCHAR(50) NOT NULL COMMENT '角色名称',
    description VARCHAR(200) COMMENT '角色描述',
    status TINYINT DEFAULT 1 COMMENT '角色状态：1-启用，0-禁用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by BIGINT COMMENT '创建人ID',
    update_by BIGINT COMMENT '更新人ID',
    INDEX idx_role_code (role_code)
) COMMENT='角色表';

-- 3. 用户角色关联表
CREATE TABLE sys_user_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    create_by BIGINT COMMENT '创建人ID',
    UNIQUE KEY uk_user_role (user_id, role_id),
    INDEX idx_user_id (user_id),
    INDEX idx_role_id (role_id)
) COMMENT='用户角色关联表';

-- 4. 部门表
CREATE TABLE sys_department (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '部门ID',
    dept_code VARCHAR(50) NOT NULL UNIQUE COMMENT '部门编码',
    dept_name VARCHAR(100) NOT NULL COMMENT '部门名称',
    parent_id BIGINT DEFAULT 0 COMMENT '父部门ID',
    level INT DEFAULT 1 COMMENT '部门层级',
    sort INT DEFAULT 0 COMMENT '排序',
    status TINYINT DEFAULT 1 COMMENT '部门状态：1-启用，0-禁用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by BIGINT COMMENT '创建人ID',
    update_by BIGINT COMMENT '更新人ID',
    INDEX idx_dept_code (dept_code),
    INDEX idx_parent_id (parent_id)
) COMMENT='部门表';

-- 5. 答辩组长表
CREATE TABLE defense_leader (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    teacher_id BIGINT NOT NULL COMMENT '教师ID',
    year INT NOT NULL COMMENT '年份',
    department_id BIGINT NOT NULL COMMENT '部门ID',
    status TINYINT DEFAULT 1 COMMENT '状态：1-有效，0-无效',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by BIGINT COMMENT '创建人ID',
    update_by BIGINT COMMENT '更新人ID',
    UNIQUE KEY uk_teacher_year (teacher_id, year),
    INDEX idx_teacher_id (teacher_id),
    INDEX idx_year (year),
    INDEX idx_department_id (department_id)
) COMMENT='答辩组长表';

-- 初始化数据

-- 插入角色数据
INSERT INTO sys_role (role_code, role_name, description) VALUES
('SUPER_ADMIN', '超级管理员', '系统超级管理员，拥有所有权限'),
('DEPT_ADMIN', '院系管理员', '院系管理员，管理本院系相关事务'),
('DEFENSE_LEADER', '答辩组长', '答辩组长，负责答辩组织管理'),
('TEACHER', '教师', '普通教师用户');

-- 插入超级管理员用户
INSERT INTO sys_user (username, password, real_name, status) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iKTcKBhKKDQUP9fhpfYBKnbm6Ei2', '超级管理员', 1);

-- 为超级管理员分配角色
INSERT INTO sys_user_role (user_id, role_id) VALUES
(1, 1);