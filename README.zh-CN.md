# 毕业答辩管理系统

[English](README.md)

## 项目简介
本项目是基于 Spring Boot、MyBatis、Thymeleaf 的毕业答辩管理系统，覆盖院系配置、师生互选、分组打分、成绩计算、文档导出与归档全流程。

系统支持按年份隔离数据，并基于角色实现权限控制。

## 角色与权限
| 角色 | 关键权限 |
|---|---|
| `SUPER_ADMIN` | 全局管理院系、用户、系统配置与跨年份数据 |
| `DEPT_ADMIN` | 管理本院系学生、教师、分组、互选与配置 |
| `DEFENSE_LEADER` | 查看本组打分、生成评语、导出本组文档 |
| `TEACHER` | 指导/评阅打分、小组打分、大组打分、导出文档 |
| `STUDENT` | 查看个人信息、填报志愿、查看录取与成绩结果 |

## 核心功能
### 1) 用户与认证
- 多角色登录、会话鉴权、权限拦截
- 支持用户与教师修改密码

### 2) 答辩流程管理
- 配置答辩年份、答辩日期、成绩评定日期
- 学生/教师/组长支持手动与随机分配
- 互选支持截止时间、轮次控制、延期与随机补分配

### 3) 评分与成绩计算
- 支持导师分、评阅分、小组答辩分、大组答辩分
- 自动计算调节系数与总评成绩
- 在线评分支持“输入总分自动拆分分项（论文/设计）”

### 4) AI评语
- 支持配置 QWEN API Key 与论文/设计提示词模板
- 支持流式与非流式评语生成

### 5) 模板与导出
- 支持 7 类 Word 模板（全局模板 + 院系覆盖）
- 支持成绩表、评定表、过程表、统分表及 ZIP 打包导出
- 支持教师/组长/系主任签名图片写入文档

## 技术栈
- 后端：Spring Boot
- 持久层：MyBatis + XML Mapper
- 数据库：MySQL
- 模板引擎：Thymeleaf
- 文档处理：Apache POI
- 构建工具：Maven

## 目录结构
```text
src/main/java/com/example/defensemanagement/
├── controller/
├── service/
├── mapper/
├── entity/
├── interceptor/
└── config/
```

## 快速开始
1. 创建 MySQL 数据库并导入 `src/main/resources/data.sql`
2. 在 `src/main/resources/application.yml` 配置数据库连接
3. 执行 `mvn spring-boot:run` 启动
4. 默认超级管理员账号：`admin / 123456`

## 相关文档
- [数据库表结构说明](数据库表结构说明.md)
- [Excel导入格式说明](Excel导入格式说明.md)
