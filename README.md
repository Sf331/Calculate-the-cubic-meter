# 课立方

面向中小型辅导机构的一站式智能运营 SaaS。

> **这是一个演示用的 demo，不是上线系统。** 数据库密码明文、账号密码明文、没有多租户、没有权限框架 —— 这些取舍都在 `编码计划书.md` 的第一节和第十一节写明了原因。

配套文档：

- [`课立方-项目计划书(1).md`](课立方-项目计划书(1).md) —— 商业计划书
- [`编码计划书.md`](编码计划书.md) —— 技术方案、数据模型、批次计划、演示动线

---

## 技术栈

| 层 | 选型 |
| --- | --- |
| 后端 | Spring Boot 3.2 + Java 21 + MyBatis-Plus + MySQL 8 |
| 前端 | Vue 3 + Vite + TypeScript + Element Plus + Pinia |
| 建表 | `schema.sql` + `data.sql`，Spring Boot 启动时自动执行 |

**不用 Docker、不用 Redis、不用对象存储、不用消息队列、不用 Spring Security、不用 Flyway。**
全部理由见 `编码计划书.md` 第二节。

---

## 目录结构

```
课立方/
├── backend/
│   ├── .env.example                    ← 复制为 .env 后改数据库密码
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/kelifang/
│       │   ├── common/                 统一返回、异常、登录拦截器、用户上下文、本地文件存储
│       │   ├── auth/                   登录、登出、当前用户
│       │   ├── basedata/               学生、家长、教师、课程、班级、教室、校区
│       │   ├── schedule/               排课 + engine 子包（求解器）
│       │   ├── attendance/             签到、课时核销、课时账户、请假补课
│       │   ├── salary/                 薪酬规则、工时、工资单
│       │   ├── finance/                资金流水、报表
│       │   ├── content/                内容资产
│       │   ├── courseware/             交互式课件与互动结果
│       │   └── homework/               作业、错题本、学情
│       └── resources/
│           ├── application.yml
│           ├── schema.sql              28 张表
│           └── data.sql                测试账号
├── frontend/
│   ├── .env.example
│   └── src/
│       ├── api/                        请求封装，每模块一个 ts
│       ├── router/                     路由 + 菜单（menus.ts 是唯一来源）
│       ├── stores/                     Pinia
│       ├── layouts/                    桌面端布局 + 手机端布局
│       └── views/                      页面
└── data/uploads/                       运行时生成，已 gitignore
```

---

## 启动

前置：JDK 21、Maven、Node 18+、MySQL 8。

```bash
# 1. 建库
mysql -uroot -p -e "CREATE DATABASE kelifang DEFAULT CHARSET utf8mb4;"

# 2. 后端
cd backend
cp .env.example .env        # 打开 .env，把 DB_PASSWORD 改成你自己的密码
mvn spring-boot:run         # 自动建表 + 灌种子数据 → http://localhost:8080

# 3. 前端（另开一个终端）
cd frontend
cp .env.example .env
npm install
npm run dev                 # → http://localhost:5173
```

**表结构会每次启动重建。** `schema.sql` 里每张表都是 `DROP TABLE IF EXISTS` 打头，`application.yml` 里 `spring.sql.init.mode=always`。所以改了表结构直接重启即可，不用手工清库，种子数据也永远是干净的初始状态。

---

## 测试账号

密码统一 `123456`。登录页有快捷登录按钮，演示时不用手敲。

| 角色 | 账号 | 能看到 |
| --- | --- | --- |
| 校长 | `principal` | 全部菜单 |
| 教务 | `academic` | 业务数据，无薪酬与财会菜单 |
| 教师 | `teacher01` | 我的课表、签到、课件、作业、我的工资单 |
| 学生 | `student01` | 我的课表、作业、错题本、学情报告 |
| 家长 | `parent01` | 孩子的课表、课时账户、作业、通知 |

学生与家长登录后是手机版布局，其余角色是桌面版。两者路由同源，只有外壳不同。

---

## 当前进度

**只搭了框架，没有业务逻辑。** 除登录外，所有页面都是占位页。

| 模块 | 状态 |
| --- | --- |
| 工程骨架、`.env` 配置、建表、登录与角色 | ✅ 已完成 |
| 基础数据（学生/教师/课程/班级/教室） | ⬜ 未开始 |
| 智能排课 + 排课引擎 | ⬜ 未开始 |
| 签到与课时核销 | ⬜ 未开始 |
| 工时薪酬 | ⬜ 未开始 |
| 财会报表 | ⬜ 未开始 |
| 内容托管 | ⬜ 未开始 |
| 交互式课件 | ⬜ 未开始 |
| 电子化作业 | ⬜ 未开始 |

开发顺序与每批次结束时该能演示什么，见 `编码计划书.md` 第八节。

---

## 开发约定

- **界面规则**：最简单的界面，字体统一宋体，不做任何视觉设计。见 [`.claude/CLAUDE.md`](.claude/CLAUDE.md)。
- **私有配置**：后端 `backend/.env`，前端 `frontend/.env`，都在 `.gitignore` 里。仓库只提交 `.env.example`。所以 `git pull` 之后如果缺 `.env`，照着 example 复制一份。
- **接口约定**：`/api/{module}/{resource}`，统一响应体 `{code, msg, data}`，`code` 为 0 表示成功。
- **前端请求**：一律用 `src/api/request.ts` 导出的 `get/post/put/del`，不要直接用 axios。
- **菜单与路由**：改 `src/router/menus.ts` 一处，菜单和路由同时生效。

---

## 已知限制（demo 专用）

这些是故意留的，不要当成 bug 修，也不要在演示时假装已经实现：

- 密码明文存储、明文比对
- 没有多租户隔离，全库只有一家机构
- 没有审计日志
- 每次重启数据库重建，演示数据不累积
- `data.sql` 只有 6 条基础数据，完整的演示数据（300 学生 / 15 教师 / 8 班级）在演示打磨阶段补
- 家长端、学生端是浏览器 H5，不是微信小程序
