# Graduation Defense Management System

[切换到中文](README.zh-CN.md)

## Overview
This project is a graduation defense management system built with Spring Boot, MyBatis, and Thymeleaf. It covers the full workflow including department setup, teacher-student preference matching, group scoring, final score calculation, document export, and archival.

The system supports year-based data isolation and role-based access control.

## Roles & Permissions
| Role | Key Permissions |
|---|---|
| `SUPER_ADMIN` | Global management of departments, users, configurations, and cross-year data |
| `DEPT_ADMIN` | Manages students, teachers, groups, matching process, and configs in own department |
| `DEFENSE_LEADER` | Reviews group scores, generates comments, exports group documents |
| `TEACHER` | Advisor/reviewer scoring, group scoring, large-group scoring, document export |
| `STUDENT` | Views profile, submits preferences, checks results |

## Core Features
### 1) User & Authentication
- Multi-role login, session authentication, and permission interception
- Password change for users and teachers

### 2) Defense Workflow
- Configure defense year, defense date, and grade date
- Manual and random assignment for students/teachers/group leaders
- Preference matching with deadline, round control, extension, and fallback random allocation

### 3) Scoring & Calculation
- Advisor score, reviewer score, group defense score, and large-group score
- Adjustment factor and automatic total grade calculation
- Online scoring supports auto-splitting item scores from a total score (Paper/Design)

### 4) AI Comment Generation
- Configurable QWEN API key and prompt templates for paper/design
- Streaming and non-streaming comment generation

### 5) Templates & Export
- 7 Word templates with global + department-level override
- Export score sheet, evaluation sheet, process sheet, summary sheet, and ZIP bundles
- Signature image insertion (teacher/leader/department head)

## Tech Stack
- Backend: Spring Boot
- Persistence: MyBatis + XML Mappers
- Database: MySQL
- Template Engine: Thymeleaf
- Document Processing: Apache POI
- Build Tool: Maven

## Project Structure
```text
src/main/java/com/example/defensemanagement/
├── controller/
├── service/
├── mapper/
├── entity/
├── interceptor/
└── config/
```

## Quick Start
1. Create MySQL database and import `src/main/resources/data.sql`
2. Configure database connection in `src/main/resources/application.yml`
3. Start with `mvn spring-boot:run`
4. Default super admin: `admin / 123456`

## Related Docs
- [Database Schema Notes](数据库表结构说明.md)
- [Excel Import Format](Excel导入格式说明.md)
