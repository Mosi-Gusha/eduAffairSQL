# 教务选课与成绩管理系统

这是一个面向学校日常教务场景的 B/S 管理系统，覆盖管理员、教师、学生三类角色，支持学期管理、在线选课、课程安排、成绩登记、成绩查询和通知公告等核心流程。

## 技术栈

- 后端：Java 17、Spring Boot 3.3.5、Spring MVC、MyBatis
- 数据库：MySQL 8.x
- 缓存：Spring Data Redis，可通过环境变量关闭
- 前端：原生 HTML、CSS、JavaScript 单页应用
- 构建：Maven

## 项目结构

以下结构不包含 `.gitignore` 已屏蔽的本地缓存、构建产物和 IDE 文件。

```text
.
├── pom.xml
├── README.md
├── .gitignore
├── database/
│   └── test.sql
└── src/
    └── main/
        ├── java/com/student/management/
        │   ├── TeachingAffairsApplication.java
        │   ├── common/          # 通用响应、异常、缓存、密码和 Map 工具
        │   ├── config/          # MVC 配置、当前用户参数解析
        │   ├── controller/      # 登录、公共、管理员、教师、学生接口
        │   ├── dto/             # 请求和响应 DTO
        │   ├── mapper/          # MyBatis SQL 映射
        │   ├── security/        # 会话、角色注解、认证拦截器
        │   └── service/         # 业务服务
        └── resources/
            ├── application.yml
            └── static/
                ├── index.html
                ├── app.js
                └── styles.css
```

## 数据库说明

首次运行前必须手动导入 `database/test.sql`。项目不会在启动时自动 `ALTER TABLE`，也不会在旧库缺少数据时自动生成历史课程或成绩。需要调整表结构或初始化数据时，请修改 SQL 后重新手动导入。

主要数据表包括：

- `users`、`user_roles`、`roles`：账号和角色
- `students`、`teachers`、`departments`、`majors`：师生和组织基础数据
- `semesters`：学期生命周期、选课起止日期、当前学期标记
- `courses`、`course_offerings`、`classrooms`、`schedules`：课程、开课和排课
- `enrollments`：学生选课记录
- `grades`：平时分、考试分、总评、绩点
- `notices`、`attendance_records`、`system_settings`：通知、考勤和系统配置

## 核心功能

### 登录与公共页面

- 按管理员、教师、学生角色登录。
- 登录页展示当前学期、功能入口和通知公告。
- 认证通过后使用服务端会话令牌访问接口。

### 管理员

- 查看教务总览、课程容量、选课统计、通知和基础数据。
- 管理用户、教师、学生、课程和开课班。
- 设置当前学期，而不是手动开关全局选课。
- 新建学期后可配置该学期课程；开始选课后学生可选课；结束选课后学生不可选课。
- 选课状态可在“开始选课”和“结束选课”之间重复切换，但仍受选课起止日期限制。
- 非选课状态下可归档当前学期；归档后该学期成为历史学期，不能再选课。
- 归档当前学期前，系统会检查上一学期成绩提交情况；上一学期存在未提交成绩时禁止归档。
- 当前未归档前禁止新建下一学期，避免多个活动学期并存。

### 教师

- 查看自己负责的课程、学生名单和统计数据。
- 教师登分只面向历史学期课程，通常是上一学期课程。
- 成绩登录表单不预填已有成绩，避免误以为系统自动给分。
- 成绩保存后自动计算总评和绩点。

### 学生

- 只能在当前学期处于选课状态且当前日期位于选课起止日期内选课。
- 当前学期不能重复选同一门课。
- 历史学期已通过课程不能重复选；历史挂科课程允许重修。
- 可查看当前课表、已选课程、可选课程和个人成绩。
- 查分面向自入学以来所有已归档且已登分的历史学期。
- 可查看成绩总表，也可按单个学期查看成绩。
- 单门课程不显示排名。
- 成绩页展示各学期平均绩点走势图。

## 学期生命周期

学期状态定义在 `semesters.status`：

| 状态 | 含义 |
| --- | --- |
| `planning` | 新建后的规划状态，可设置课程和选课日期 |
| `selecting` | 开始选课状态，学生可在选课日期窗口内选课 |
| `closed` | 结束选课状态，禁止学生继续选课，可再次开始选课 |
| `archived` | 已归档历史学期，不能再选课，可用于成绩查询和教师登分 |

管理员操作约束：

- 只能有一个未归档学期。
- 新建学期会成为当前学期。
- 开始选课、结束选课、归档学期都只能作用于当前学期。
- 归档要求当前学期不处于选课状态。
- 归档要求上一学期所有应提交成绩均已提交。

## 绩点规则

| 总评分数 | 绩点 |
| --- | --- |
| 90-100 | 4.0 |
| 85-89 | 3.7 |
| 82-84 | 3.3 |
| 78-81 | 3.0 |
| 75-77 | 2.7 |
| 72-74 | 2.3 |
| 68-71 | 2.0 |
| 66-67 | 1.7 |
| 64-65 | 1.5 |
| 60-63 | 1.0 |
| 0-59 | 0.0 |

## 完整使用方法

### 1. 准备环境

- JDK 17
- Maven 3.8+
- MySQL 8.x
- Redis 7.x，可选

### 2. 手动导入数据库

在 MySQL 命令行中执行：

```sql
SOURCE E:/Student/database/test.sql;
```

或者在系统终端执行：

```bash
mysql -u root -p < database/test.sql
```

导入后会创建 `test` 数据库、表结构、触发器、存储过程和示例数据。

### 3. 配置环境变量

可直接使用 `src/main/resources/application.yml` 中的默认值，也可以覆盖：

```bat
cmd:
set "DB_HOST=127.0.0.1"
set "DB_PORT=3306"
set "DB_NAME=test"
set "DB_USER=root"
set "DB_PASSWORD=password"
set "PORT=8000"

powershell:
$env:DB_HOST="127.0.0.1"
$env:DB_PORT="3306"
$env:DB_NAME="test"
$env:DB_USER="root"
$env:DB_PASSWORD="password"
$env:PORT="8000"
```

Redis 默认开启。如果没有 Redis，可关闭缓存：

```bat
set CACHE_ENABLED=false
```

如需连接 Redis：

```bat
cmd:
set "REDIS_HOST=127.0.0.1"
set "REDIS_PORT=6379"
set "REDIS_PASSWORD="
set "REDIS_DATABASE=0"
set "CACHE_TTL_SECONDS=300"

powershell:
$env:REDIS_HOST="127.0.0.1"
$env:REDIS_PORT="6379"
$env:REDIS_PASSWORD=""
$env:REDIS_DATABASE="0"
$env:CACHE_TTL_SECONDS="300"
```

### 4. 启动项目

开发启动：

```bash
mvn spring-boot:run
```

打包运行：

```bash
mvn -DskipTests package
java -jar target/teaching-affairs-management-1.0.0.jar
```

访问地址：

```text
http://localhost:8000
```

### 5. 默认账号

`database/test.sql` 中包含示例账号：

| 角色 | 用户名 | 密码 |
| --- | --- | --- |
| 管理员 | `admin` | `admin` |
| 教师 | `t1` | `t1` |
| 教师 | `t2` | `t2` |
| 教师 | `t3` | `t3` |
| 学生 | `s1` | `s1` |
| 学生 | `s2` | `s2` |
| 学生 | `s3` | `s3` |

## 常用验证命令

前端脚本语法检查：

```bash
node --check src/main/resources/static/app.js
```

Java 打包检查：

```bash
mvn -DskipTests package
```

如果需要把 Maven 本地仓库放在项目目录内：

```bash
mvn "-Dmaven.repo.local=E:\Student\.m2\repository" -DskipTests package
```
