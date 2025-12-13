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
