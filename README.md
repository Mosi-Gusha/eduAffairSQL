# 教务选课与成绩管理系统

这是一个面向学校日常教务场景的 B/S 管理系统，覆盖管理员、教师、学生三类角色，支持学期维护、课程基础信息维护、课程班安排、在线选课、成绩登记、成绩查询和通知公告等流程。

## 技术栈

- 后端：Java 17、Spring Boot 3.3.5、Spring MVC、MyBatis
- 数据库：MySQL 8.x
- 缓存：Spring Data Redis，可通过环境变量关闭
- 前端：原生 HTML、CSS、JavaScript 单页应用
- 构建：Maven

## 项目结构

```text
.
├── pom.xml
├── README.md
├── database/
│   └── test.sql
└── src/
    └── main/
        ├── java/com/student/management/
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

首次运行前需要手动导入 `database/test.sql`。项目不会在启动时自动 `ALTER TABLE`，需要调整表结构或初始化数据时，请修改 SQL 后重新导入。

主要数据表：

- `roles`、`users`：账号、角色和账号状态。
- `departments`、`majors`、`students`、`teachers`：院系、专业、学生和教师基础信息。
- `semesters`：学期名称、开始日期、结束日期和最大学分。
- `courses`：课程基础信息，包括课程号、课程名、开课院系、学分和课程状态
- `course_offerings`、`course_offering_times`、`classrooms`：课程班、课程班上课时间段、教室和排课信息。
- `enrollments`：学生选课记录。
- `grades`：平时分、考试分、最终成绩和绩点。最终成绩为整数。
- `notices`：通知公告。

## 核心功能

### 登录与公共页面

- 按管理员、教师、学生角色登录。
- 登录页展示当前学期、功能入口和通知公告。
- 认证通过后使用服务端会话令牌访问接口。

### 管理员

- 查看教务总览、课程容量、选课统计、通知和基础数据。
- 维护学期：新增学期、编辑自动判定出的当前学期起止日期和最大学分。
- 维护课程：查看课程号、课程名、开课院系、学分、课程状态；可新增课程、启用课程、弃用课程，不提供编辑或删除。
- 维护课程班：为当前学期新增和调整课程班，课程班包含教师、教室、一个或多个上课时间段、容量和平时/考试比例；课程班选课名单中可由管理员手动删除学生选课记录。
- 弃用课程不能再新建课程班；已创建的历史课程班不受影响。
- 维护学生和教师：可新增、修改、启用或弃用账号；弃用只限制登录，不清除历史选课、授课和成绩数据。学生管理展示学院、专业、学生邮箱、入学年份和账号状态。
- 发布、编辑和删除通知。

### 教师

- 查看自己负责的课程班、学生名单和统计数据。
- 成绩登记面向可录入成绩的课程班，录入平时分和考试分后自动计算最终成绩与绩点。
- 最终成绩按比例计算后四舍五入为整数，绩点按四舍五入后的最终成绩计算。

### 学生

- 只在当前日期位于当前学期起止日期内时选课。
- 当前学期不能重复选同一门课程。
- 历史学期已通过课程不能重复选；历史挂科课程允许重修。
- 选课时检查容量、学分上限、时间冲突和单双周冲突；学分上限取自所在学期的最大学分。
- 可查看当前课表、已选课程、可选课程、个人成绩和绩点趋势。
- 学生首页平均绩点按全部学期已出成绩课程的学分加权平均计算。

## 学期状态规则

`semesters` 表不保存状态字段，状态由当前日期和学期起止日期动态计算：

| 状态 | 计算规则 | 含义 |
| --- | --- | --- |
| `not_started` | 当前日期早于 `start_date` | 未开始 |
| `active` | 当前日期位于 `start_date` 和 `end_date` 之间，含首尾日期 | 进行中 |
| `archived` | 当前日期晚于 `end_date` | 已归档 |

系统不存储当前学期标记。当前学期按系统日期自动判定：优先选择当前日期位于起止日期内的进行中学期；如果当前日期处于两个学期之间，则选择未来开始日期最早的学期；如果所有学期均已结束，则选择最近结束的学期。`max_credit` 保存该学期的选课最大学分。选课除要求课程班状态为 `selecting` 外，还要求目标学期是自动判定出的当前学期且当前日期位于该学期起止日期内。

## 课程与课程班规则

- 课程是基础数据，只包含课程号、课程名、开课院系、学分和状态。
- 课程可以启用或弃用。弃用只影响后续新建课程班，不影响已有课程班、历史选课和历史成绩。
- 课程班是具体开课安排，关联课程、学期、教师、教室、容量和成绩比例；上课时间段独立保存在 `course_offering_times`。
- 一个课程班至少需要一个上课时间段，每个时间段包含星期、开始节、结束节、起始周、结束周和单双周类型。
- 时间段单双周使用枚举：`all` 表示全部，`odd` 表示单周，`even` 表示双周。
- 时间冲突检查会同时考虑星期、节次、起止周和单双周；全部周次与单周/双周均视为冲突，单周和双周可共用同一时段。

## 成绩与绩点规则

最终成绩由触发器按课程班平时/考试比例自动计算：

```text
最终成绩 = ROUND(平时分 * 平时比例 + 考试分 * 考试比例, 0)
```

数据库中 `grades.final_score` 为 `DECIMAL(5,0)`，因此最终成绩保存为整数。绩点按四舍五入后的最终成绩计算。

| 最终成绩 | 绩点 |
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

## 使用方法

### 1. 准备环境

- JDK 17
- Maven 3.8+
- MySQL 8.x
- Redis 7.x，可选

### 2. 导入数据库

在 MySQL 命令行中执行：

```sql
SOURCE E:/Student/database/test.sql;
```

或在系统终端执行：

```bash
mysql -u root -p < database/test.sql
```

导入后会创建 `test` 数据库、表结构、触发器、存储过程和示例数据。

### 3. 配置环境变量

可直接使用 `src/main/resources/application.yml` 中的默认值，也可以覆盖：

cmd：

```bat
set "DB_HOST=127.0.0.1"
set "DB_PORT=3306"
set "DB_NAME=test"
set "DB_USER=root"
set "DB_PASSWORD=password"
set "PORT=8000"
set "REDIS_HOST=127.0.0.1"
set "REDIS_PORT=6379"
set "REDIS_PASSWORD="
set "REDIS_DATABASE=0"
set "CACHE_ENABLED=true"
set "CACHE_TTL_SECONDS=300"
set "CACHE_REDIS_BACKOFF_SECONDS=30"
```

PowerShell：

```powershell
$env:DB_HOST="127.0.0.1"
$env:DB_PORT="3306"
$env:DB_NAME="test"
$env:DB_USER="root"
$env:DB_PASSWORD="password"
$env:PORT="8000"
$env:REDIS_HOST="127.0.0.1"
$env:REDIS_PORT="6379"
$env:REDIS_PASSWORD=""
$env:REDIS_DATABASE="0"
$env:CACHE_ENABLED="true"
$env:CACHE_TTL_SECONDS="300"
$env:CACHE_REDIS_BACKOFF_SECONDS="30"
```

Redis 默认启用。如果没有 Redis，可关闭缓存：

cmd：

```bat
set "CACHE_ENABLED=false"
```

PowerShell：

```powershell
$env:CACHE_ENABLED="false"
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

Java 编译：

```bash
mvn "-Dmaven.repo.local=E:\Student\.m2\repository" compile
```

运行测试：

```bash
mvn "-Dmaven.repo.local=E:\Student\.m2\repository" test
```
