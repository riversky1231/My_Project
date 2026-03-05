# 毕业答辩管理系统 | Graduation Defense Management System

## 项目简介 | Overview
本项目是基于 Spring Boot + MyBatis + Thymeleaf 的毕业答辩管理系统，覆盖院系配置、师生互选、分组打分、成绩汇总、文档导出与归档全流程。  
This project is a graduation defense management system built with Spring Boot + MyBatis + Thymeleaf, covering department configuration, teacher-student matching, group scoring, score aggregation, document export, and archiving.

系统支持按年份隔离数据，并通过角色权限控制不同用户可见范围与可操作能力。  
The system supports year-based data isolation and role-based permissions for visibility and operations.

## 角色与权限 | Roles & Permissions
| 中文角色 | Role | 权限摘要 (CN) | Permission Summary (EN) |
|---|---|---|---|
| 超级管理员 | `SUPER_ADMIN` | 全局管理院系、用户、配置与跨年份数据 | Manages departments, users, configs, and cross-year data globally |
| 院系管理员 | `DEPT_ADMIN` | 管理本院系学生/教师/分组/互选/配置 | Manages students, teachers, groups, matching, and configs in own department |
| 答辩组长 | `DEFENSE_LEADER` | 查看本组打分、生成评语、导出本组文档 | Views group scores, generates comments, exports group docs |
| 教师 | `TEACHER` | 指导/评阅打分、小组打分、大组打分、导出相关文档 | Advisor/reviewer scoring, group scoring, large-group scoring, export docs |
| 学生 | `STUDENT` | 查看个人信息、填报志愿、查看结果 | Views own profile, submits preferences, checks results |

## 核心功能 | Core Features
### 1) 用户与认证 | User & Authentication
- 多角色登录、会话鉴权、权限拦截。  
  Multi-role login, session auth, and permission interception.
- 支持用户与教师改密。  
  Supports password change for users and teachers.

### 2) 答辩流程管理 | Defense Workflow
- 院系管理员维护答辩年份、答辩日期、成绩日期。  
  Department admins maintain defense year, defense date, and grade date.
- 小组管理支持手动分配和随机分配（学生/教师/组长）。  
  Group management supports both manual and random assignment (students/teachers/leaders).
- 志愿互选支持截止时间、轮次控制、延迟与随机补分配。  
  Teacher-student preference matching supports deadlines, rounds, extensions, and random fallback allocation.

### 3) 评分与成绩计算 | Scoring & Calculation
- 支持指导成绩、评阅成绩、小组答辩成绩、大组答辩成绩。  
  Supports advisor score, reviewer score, group defense score, and large-group defense score.
- 支持调节系数与最终答辩成绩、总评成绩自动计算。  
  Supports adjustment factor and automatic calculation of final defense score and total grade.
- 新增：在线评分支持“输入总分自动生成分项（论文/设计）”。  
  New: online scoring supports "enter total score to auto-split item scores" for both paper/design types.

### 4) AI评语 | AI Comment Generation
- 支持配置 QWEN API Key 与论文/设计提示词模板。  
  Supports configurable QWEN API key and prompt templates for paper/design.
- 支持流式与非流式评语生成。  
  Supports both streaming and non-streaming comment generation.

### 5) 文档模板与导出 | Template & Export
- 支持 7 类 Word 模板上传（全局模板 + 院系模板覆盖）。  
  Supports 7 Word templates upload (global + department-specific override).
- 支持成绩表、评定表、过程表、统分表导出及批量打包。  
  Supports export of score sheets, evaluation sheets, process sheets, summary sheets, and ZIP bundles.
- 支持教师/组长/系主任签名图片合成到文档。  
  Supports signature image insertion (teacher/leader/department head) into generated documents.

## 技术栈 | Tech Stack
- Backend: Spring Boot
- Persistence: MyBatis + XML Mapper
- Database: MySQL
- Template Engine: Thymeleaf
- Document Processing: Apache POI
- Build Tool: Maven

## 目录结构 | Project Structure
```text
src/main/java/com/example/defensemanagement/
├── controller/    # 控制器层 | controllers
├── service/       # 业务层 | services
├── mapper/        # 数据访问层 | mappers
├── entity/        # 实体定义 | entities
├── interceptor/   # 拦截器 | interceptors
└── config/        # 系统配置 | configuration
```

## 快速开始 | Quick Start
1. 创建 MySQL 数据库并导入 `src/main/resources/data.sql`。  
   Create MySQL database and import `src/main/resources/data.sql`.
2. 修改 `src/main/resources/application.yml` 中数据库连接。  
   Configure DB connection in `src/main/resources/application.yml`.
3. 启动项目：`mvn spring-boot:run`。  
   Start project with `mvn spring-boot:run`.
4. 默认超级管理员：`admin / 123456`。  
   Default super admin: `admin / 123456`.

## 相关文档 | Related Docs
- [数据库表结构说明.md](数据库表结构说明.md) - 数据表与字段说明 | DB schema and field notes
- [Excel导入格式说明.md](Excel导入格式说明.md) - Excel导入规范 | Excel import format
