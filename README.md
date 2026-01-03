# 毕业设计答辩管理系统 (Graduation Defense Management System)

## 📖 项目简介
本项目是一个基于 Spring Boot 和 MyBatis 的全流程毕业设计答辩管理系统。系统旨在数字化管理高校毕业答辩的各个环节，包括学生分组、导师分配、现场答辩评分、成绩汇总以及文档归档。

本项目的一个核心特色是将**经典设计模式**（如单例、观察者、工厂、代理模式等）应用于实际的业务场景中，以提高系统的可扩展性和代码质量。

## 🚀 系统核心功能

### 1. 用户与权限管理 (User & Auth)
* **多角色支持**：系统支持管理员、答辩秘书（教师）、答辩组长、普通教师和学生等多种角色。
* **安全认证**：基于拦截器（`AuthInterceptor`）和 Spring Security 配置实现的用户登录与权限控制。
* **电子签名**：支持用户（如答辩组长）上传和管理电子签名，用于生成文档。

### 2. 答辩流程管理 (Defense Process)
* **答辩分组**：支持创建答辩小组（`DefenseGroup`），并分配组长、答辩秘书及组员。
* **人员分配**：
    * **学生管理**：导入学生信息，分配至对应答辩组。
    * **教师管理**：分配教师至答辩组，设定教师角色（如指导老师、评阅老师、答辩评委）。
* **流程控制**：管理答辩的各个阶段（准备、进行中、结束、归档）。

### 3. 评分与成绩体系 (Scoring System)
这是系统的核心业务模块，支持多维度的成绩计算：
* **多角色评分**：
    * **指导教师评分**：针对学生的平时表现和论文质量打分。
    * **评阅教师评分**：针对论文的学术水平打分。
    * **现场答辩评分**：答辩小组老师在答辩现场进行打分。
* **成绩汇总**：自动计算小组评分、大组评分，并根据权重合成**学生最终成绩** (`StudentFinalScore`)。
* **评分记录**：详细记录每位老师的评分历史 (`TeacherScoreRecord`)。

### 4. 智能辅助与反馈 (AI Integration)
* **AI 评语生成**：集成了 AI 服务 (`AiCommentService`)，能够根据学生的答辩表现或论文内容，辅助老师生成答辩评语，提高工作效率。

### 5. 文档与数据管理 (Document & Data)
* **数据导出**：
    * **Excel 导出**：支持导出学生名单、成绩单等数据。
    * **Word 文档生成**：利用模板引擎 (`DocTemplateService`) 自动生成官方格式的文档，如《答辩记录表》、《答辩评分表》、《答辩决议书》等。
* **文档归档**：支持答辩全过程文档的归档管理 (`ArchiveSession`)。

---

## 🛠️ 设计模式应用 (Design Patterns)

本项目在 `com.example.defensemanagement.pattern` 包下展示了多种设计模式的实战应用：

| 模式名称 | 应用场景 | 实现类/位置 |
| :--- | :--- | :--- |
| **工厂模式 (Factory)** |用于创建不同格式的导出服务（Excel导出/Word导出），解耦导出逻辑。 | `ExportServiceFactory`, `ExcelExportService`, `WordExportService` |
| **观察者模式 (Observer)** | 当学生成绩发布或修改时，自动触发日志记录和邮件通知，实现业务解耦。 | `ScoreSubject`, `ScoreObserver`, `EmailNotificationObserver`, `LogObserver` |
| **单例模式 (Singleton)** | 保证系统日志记录器在全局只有一个实例，节省资源。 | `SystemLogger` |
| **代理模式 (Proxy)** | 为核心服务添加额外的非业务逻辑（如权限检查、性能监控），而不修改原有代码。 | `ProxyFactory`, `ServiceProxy` |

---

## 🏗️ 技术栈 (Tech Stack)

* **后端框架**: Spring Boot
* **持久层**: MyBatis (配合 XML Mapper)
* **数据库**: MySQL
* **模板引擎**: Thymeleaf (用于生成部分前端页面或文档)
* **文档处理**: Apache POI (推测用于Excel/Word处理)
* **构建工具**: Maven

## 📂 目录结构摘要

```text
src/main/java/com/example/defensemanagement/
├── controller/          # 控制器层 (API接口)
├── service/             # 业务逻辑层
├── entity/              # 数据库实体类
├── mapper/              # MyBatis Mapper接口
├── pattern/             # 设计模式实现包
│   ├── factory/         # 工厂模式
│   ├── observer/        # 观察者模式
│   ├── proxy/           # 代理模式
│   └── singleton/       # 单例模式
└── config/              # 系统配置