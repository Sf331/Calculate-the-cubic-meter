-- 课立方 demo 数据库结构
--
-- 每次启动都会整库重建（application.yml 里 spring.sql.init.mode=always）。
-- 因此每张表都以 DROP TABLE IF EXISTS 打头，重启即得到干净的初始状态。
--
-- 刻意不加外键约束：demo 阶段改表结构是常态，外键会让 DROP/重建和数据导入变麻烦，
-- 完整性由 Service 层保证。表格顺序因此也不重要。

SET NAMES utf8mb4;

-- ============================================================
-- 5.1 账号与组织
-- ============================================================

DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    username    VARCHAR(50)  NOT NULL COMMENT '登录账号',
    password    VARCHAR(100) NOT NULL COMMENT 'demo 阶段明文存储，上线前必须改为哈希',
    real_name   VARCHAR(50)  NOT NULL COMMENT '姓名',
    role        VARCHAR(20)  NOT NULL COMMENT 'PRINCIPAL/ACADEMIC/TEACHER/STUDENT/PARENT',
    phone       VARCHAR(20)  DEFAULT NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账号（五类角色共用）';

DROP TABLE IF EXISTS campus;
CREATE TABLE campus (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    name        VARCHAR(50) NOT NULL COMMENT '校区名',
    address     VARCHAR(200) DEFAULT NULL,
    created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='校区';

-- ============================================================
-- 5.2 基础数据
-- ============================================================

DROP TABLE IF EXISTS teacher;
CREATE TABLE teacher (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    user_id      BIGINT      DEFAULT NULL COMMENT '关联 sys_user，未开通账号的教师可为空',
    name         VARCHAR(50) NOT NULL,
    subject      VARCHAR(50) DEFAULT NULL COMMENT '所授科目',
    campus_id    BIGINT      DEFAULT NULL,
    salary_type  VARCHAR(20) NOT NULL DEFAULT 'HOURLY' COMMENT 'HOURLY/BASE_HOURLY/MONTHLY',
    base_salary  DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '底薪，仅 BASE_HOURLY/MONTHLY 用',
    deleted      TINYINT     NOT NULL DEFAULT 0,
    created_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教师';

DROP TABLE IF EXISTS student;
CREATE TABLE student (
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    user_id         BIGINT      DEFAULT NULL,
    parent_user_id  BIGINT      DEFAULT NULL COMMENT '关联家长账号',
    name            VARCHAR(50) NOT NULL,
    grade           VARCHAR(20) DEFAULT NULL COMMENT '年级',
    campus_id       BIGINT      DEFAULT NULL,
    deleted         TINYINT     NOT NULL DEFAULT 0,
    created_at      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_parent (parent_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生';

DROP TABLE IF EXISTS course;
CREATE TABLE course (
    id                BIGINT      NOT NULL AUTO_INCREMENT,
    name              VARCHAR(100) NOT NULL,
    subject           VARCHAR(50)  DEFAULT NULL,
    grade             VARCHAR(20)  DEFAULT NULL,
    duration_minutes  INT          NOT NULL DEFAULT 45 COMMENT '单次课时长',
    weekly_times      INT          NOT NULL DEFAULT 1 COMMENT '周频次',
    tags              JSON         DEFAULT NULL COMMENT '特殊课型标记：TRIAL/MAKEUP/SUBSTITUTE/CROSS_CAMPUS',
    deleted           TINYINT      NOT NULL DEFAULT 0,
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程';

DROP TABLE IF EXISTS classroom;
CREATE TABLE classroom (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    campus_id   BIGINT      DEFAULT NULL,
    name        VARCHAR(50) NOT NULL,
    capacity    INT         NOT NULL DEFAULT 0,
    deleted     TINYINT     NOT NULL DEFAULT 0,
    created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教室';

DROP TABLE IF EXISTS clazz;
CREATE TABLE clazz (
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    name          VARCHAR(100) NOT NULL,
    course_id     BIGINT      DEFAULT NULL,
    teacher_id    BIGINT      DEFAULT NULL,
    classroom_id  BIGINT      DEFAULT NULL,
    campus_id     BIGINT      DEFAULT NULL,
    capacity      INT         NOT NULL DEFAULT 0,
    deleted       TINYINT     NOT NULL DEFAULT 0,
    created_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_course (course_id),
    KEY idx_teacher (teacher_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='班级';

DROP TABLE IF EXISTS class_student;
CREATE TABLE class_student (
    id          BIGINT   NOT NULL AUTO_INCREMENT,
    class_id    BIGINT   NOT NULL,
    student_id  BIGINT   NOT NULL,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_class_student (class_id, student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='班级学生名单';

DROP TABLE IF EXISTS teacher_availability;
CREATE TABLE teacher_availability (
    id          BIGINT   NOT NULL AUTO_INCREMENT,
    teacher_id  BIGINT   NOT NULL,
    weekday     TINYINT  NOT NULL COMMENT '1=周一 … 7=周日',
    start_time  TIME     NOT NULL,
    end_time    TIME     NOT NULL,
    PRIMARY KEY (id),
    KEY idx_teacher (teacher_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教师可用时段';

DROP TABLE IF EXISTS student_constraint;
CREATE TABLE student_constraint (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    student_id  BIGINT      NOT NULL,
    type        VARCHAR(20) NOT NULL COMMENT 'AVAILABLE=可排时段 / UNAVAILABLE=绝对不可排时段',
    weekday     TINYINT     NOT NULL,
    start_time  TIME        NOT NULL,
    end_time    TIME        NOT NULL,
    PRIMARY KEY (id),
    KEY idx_student (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生排课约束';

-- ============================================================
-- 5.3 排课
-- ============================================================

DROP TABLE IF EXISTS schedule;
CREATE TABLE schedule (
    id            BIGINT   NOT NULL AUTO_INCREMENT,
    class_id      BIGINT   NOT NULL,
    teacher_id    BIGINT   NOT NULL,
    classroom_id  BIGINT   DEFAULT NULL,
    campus_id     BIGINT   DEFAULT NULL,
    lesson_date   DATE     NOT NULL,
    start_time    TIME     NOT NULL,
    end_time      TIME     NOT NULL,
    status        VARCHAR(20) NOT NULL DEFAULT 'PLANNED' COMMENT 'PLANNED/CANCELLED/DONE',
    locked        TINYINT  NOT NULL DEFAULT 0 COMMENT '增量重排的锚点：locked=1 的行在重排时原样保留',
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_class_date (class_id, lesson_date),
    KEY idx_teacher_date (teacher_id, lesson_date),
    KEY idx_classroom_date (classroom_id, lesson_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='排课结果（四类课表均由本表派生）';

-- ============================================================
-- 5.4 签到与课时
-- ============================================================

DROP TABLE IF EXISTS attendance;
CREATE TABLE attendance (
    id               BIGINT      NOT NULL AUTO_INCREMENT,
    schedule_id      BIGINT      NOT NULL,
    student_id       BIGINT      NOT NULL,
    status           VARCHAR(20) NOT NULL COMMENT 'PRESENT/LATE/EARLY_LEAVE/LEAVE/ABSENT',
    sign_method      VARCHAR(20) NOT NULL DEFAULT 'TEACHER' COMMENT 'TEACHER=教师点名 / CODE=学生口令',
    sign_time        DATETIME    DEFAULT NULL,
    consumed_hours   DECIMAL(5,2) NOT NULL DEFAULT 0 COMMENT '本次核销课时，由状态对应的规则算出',
    created_at       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_schedule_student (schedule_id, student_id),
    KEY idx_student (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='签到记录（全链路凭证起点）';

DROP TABLE IF EXISTS lesson_account;
CREATE TABLE lesson_account (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    student_id        BIGINT       NOT NULL,
    course_id         BIGINT       NOT NULL,
    total_hours       DECIMAL(8,2) NOT NULL DEFAULT 0 COMMENT '累计购买课时',
    consumed_hours    DECIMAL(8,2) NOT NULL DEFAULT 0 COMMENT '累计消耗课时',
    remaining_hours   DECIMAL(8,2) NOT NULL DEFAULT 0 COMMENT '冗余字段，必须等于 lesson_transaction.hours 之和',
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_student_course (student_id, course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课时账户';

DROP TABLE IF EXISTS lesson_transaction;
CREATE TABLE lesson_transaction (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    student_id     BIGINT       NOT NULL,
    account_id     BIGINT       NOT NULL,
    type           VARCHAR(20)  NOT NULL COMMENT 'RECHARGE/CONSUME/REFUND/GIFT/ADJUST',
    hours          DECIMAL(8,2) NOT NULL COMMENT '带正负号',
    balance_after  DECIMAL(8,2) NOT NULL COMMENT '本次变动后的余额',
    ref_id         BIGINT       DEFAULT NULL COMMENT '来源单据 id，如 attendance.id',
    remark         VARCHAR(200) DEFAULT NULL,
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_student (student_id),
    KEY idx_account (account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课时流水（只增不改，余额由本表累加得出）';

DROP TABLE IF EXISTS leave_request;
CREATE TABLE leave_request (
    id                 BIGINT      NOT NULL AUTO_INCREMENT,
    student_id         BIGINT      NOT NULL,
    schedule_id        BIGINT      NOT NULL,
    reason             VARCHAR(200) DEFAULT NULL,
    status             VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED',
    deduct_hours       TINYINT     NOT NULL DEFAULT 0 COMMENT '审批结果：本次请假是否扣课时',
    approver_id        BIGINT      DEFAULT NULL,
    makeup_schedule_id BIGINT      DEFAULT NULL COMMENT '补课安排落定后指向新的 schedule',
    created_at         DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_student (student_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='请假与补课';

-- ============================================================
-- 5.5 薪酬
-- ============================================================

DROP TABLE IF EXISTS salary_rule;
CREATE TABLE salary_rule (
    id             BIGINT   NOT NULL AUTO_INCREMENT,
    teacher_id     BIGINT   DEFAULT NULL COMMENT 'NULL 表示机构默认规则',
    tier_config    JSON     DEFAULT NULL COMMENT '班型/人数分档单价',
    special_rate   JSON     DEFAULT NULL COMMENT '特殊课型差异单价或补贴',
    bonus_config   JSON     DEFAULT NULL COMMENT '奖励项',
    deduct_config  JSON     DEFAULT NULL COMMENT '扣款项',
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_teacher (teacher_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='计酬规则';

DROP TABLE IF EXISTS workhour_record;
CREATE TABLE workhour_record (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    teacher_id    BIGINT        NOT NULL,
    schedule_id   BIGINT        NOT NULL,
    attendance_id BIGINT        DEFAULT NULL COMMENT '追溯到产生本工时的签到记录',
    work_date     DATE          NOT NULL,
    student_count INT           NOT NULL DEFAULT 0 COMMENT '实际出勤人数，用于落入单价档位',
    rate          DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '本次应用的课时单价',
    amount        DECIMAL(10,2) NOT NULL DEFAULT 0,
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_teacher_date (teacher_id, work_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工时明细（签到核销时自动生成）';

DROP TABLE IF EXISTS payslip;
CREATE TABLE payslip (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    teacher_id    BIGINT        NOT NULL,
    period        VARCHAR(7)    NOT NULL COMMENT 'yyyy-MM',
    total_amount  DECIMAL(10,2) NOT NULL DEFAULT 0,
    detail        JSON          DEFAULT NULL COMMENT '生成时的逐项快照，之后改规则不影响历史工资单',
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_teacher_period (teacher_id, period)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工资单';

-- ============================================================
-- 5.6 财会
-- ============================================================

DROP TABLE IF EXISTS fund_transaction;
CREATE TABLE fund_transaction (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    student_id  BIGINT        DEFAULT NULL,
    type        VARCHAR(20)   NOT NULL COMMENT 'PRE_RECEIVE=收取预收款 / RECEIVE_CONFIRM=课时消耗确认收入 / REFUND=退费 / EXPENSE=支出',
    amount      DECIMAL(10,2) NOT NULL,
    direction   VARCHAR(3)    NOT NULL COMMENT 'IN/OUT',
    ref_id      BIGINT        DEFAULT NULL COMMENT '来源单据 id',
    occur_date  DATE          NOT NULL,
    remark      VARCHAR(200)  DEFAULT NULL,
    created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_student (student_id),
    KEY idx_type_date (type, occur_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资金流水（只增不改）';

-- ============================================================
-- 5.7 内容与课件
-- ============================================================

DROP TABLE IF EXISTS content_asset;
CREATE TABLE content_asset (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    name             VARCHAR(200) NOT NULL,
    subject          VARCHAR(50)  DEFAULT NULL,
    grade            VARCHAR(20)  DEFAULT NULL,
    course_id        BIGINT       DEFAULT NULL COMMENT '按课表推送时按本字段匹配',
    knowledge_point  VARCHAR(100) DEFAULT NULL,
    type             VARCHAR(20)  NOT NULL DEFAULT 'HANDOUT' COMMENT 'COURSEWARE/HANDOUT/PAPER/MEDIA',
    file_path        VARCHAR(300) DEFAULT NULL,
    version_no       INT          NOT NULL DEFAULT 1 COMMENT '回滚即把本字段指回旧值',
    scope            VARCHAR(20)  NOT NULL DEFAULT 'ALL' COMMENT 'ALL/SUBJECT_GROUP/PRIVATE',
    owner_id         BIGINT       DEFAULT NULL,
    deleted          TINYINT      NOT NULL DEFAULT 0,
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_course (course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='内容资产';

DROP TABLE IF EXISTS courseware;
CREATE TABLE courseware (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    name         VARCHAR(200) NOT NULL,
    schema_json  LONGTEXT     DEFAULT NULL COMMENT '课件 JSON：{version, pages:[{id,title,components:[...]}]}',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='交互式课件';

DROP TABLE IF EXISTS courseware_record;
CREATE TABLE courseware_record (
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    courseware_id  BIGINT      NOT NULL,
    schedule_id    BIGINT      DEFAULT NULL,
    student_id     BIGINT      NOT NULL,
    component_id   VARCHAR(50) DEFAULT NULL COMMENT '对应课件 JSON 里组件的 id',
    answer         JSON        DEFAULT NULL,
    correct        TINYINT     DEFAULT NULL,
    created_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_courseware (courseware_id),
    KEY idx_student (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课件互动结果回流';

-- ============================================================
-- 5.8 作业与学情
-- ============================================================

DROP TABLE IF EXISTS question;
CREATE TABLE question (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    subject          VARCHAR(50)  DEFAULT NULL,
    grade            VARCHAR(20)  DEFAULT NULL,
    knowledge_point  VARCHAR(100) DEFAULT NULL COMMENT '错题本按本字段聚类',
    type             VARCHAR(20)  NOT NULL COMMENT 'SINGLE/MULTI/BLANK/SUBJECTIVE',
    stem             TEXT,
    options          JSON         DEFAULT NULL,
    answer           JSON         DEFAULT NULL COMMENT '多答案时为数组，命中任一即判对',
    score            DECIMAL(5,2) NOT NULL DEFAULT 0,
    deleted          TINYINT      NOT NULL DEFAULT 0,
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_knowledge (knowledge_point)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题库';

DROP TABLE IF EXISTS homework;
CREATE TABLE homework (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    class_id      BIGINT       NOT NULL,
    name          VARCHAR(200) NOT NULL,
    question_ids  JSON         DEFAULT NULL COMMENT '题目 id 有序数组',
    total_score   DECIMAL(6,2) NOT NULL DEFAULT 0,
    due_time      DATETIME     DEFAULT NULL,
    publisher_id  BIGINT       DEFAULT NULL,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_class (class_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作业';

DROP TABLE IF EXISTS homework_submission;
CREATE TABLE homework_submission (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    homework_id       BIGINT       NOT NULL,
    student_id        BIGINT       NOT NULL,
    submit_time       DATETIME     DEFAULT NULL,
    attachment_paths  JSON         DEFAULT NULL COMMENT '图片提交的本地相对路径数组',
    score             DECIMAL(6,2) DEFAULT NULL,
    status            VARCHAR(20)  NOT NULL DEFAULT 'SUBMITTED' COMMENT 'SUBMITTED/GRADED',
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_homework_student (homework_id, student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作业提交';

DROP TABLE IF EXISTS submission_answer;
CREATE TABLE submission_answer (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    submission_id  BIGINT       NOT NULL,
    question_id    BIGINT       NOT NULL,
    answer         JSON         DEFAULT NULL,
    score          DECIMAL(5,2) DEFAULT NULL,
    correct        TINYINT      DEFAULT NULL COMMENT '客观题自动判分结果，主观题为 NULL',
    comment        VARCHAR(500) DEFAULT NULL COMMENT '教师评语',
    PRIMARY KEY (id),
    KEY idx_submission (submission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='逐题作答与批改';

DROP TABLE IF EXISTS wrong_question;
CREATE TABLE wrong_question (
    id               BIGINT      NOT NULL AUTO_INCREMENT,
    student_id       BIGINT      NOT NULL,
    question_id      BIGINT      NOT NULL,
    knowledge_point  VARCHAR(100) DEFAULT NULL,
    wrong_count      INT         NOT NULL DEFAULT 1 COMMENT '反复出错的题优先推送',
    last_wrong_time  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_student_question (student_id, question_id),
    KEY idx_student (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='错题本';

-- ============================================================
-- 5.9 通知
-- ============================================================

DROP TABLE IF EXISTS notification;
CREATE TABLE notification (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    receiver_id BIGINT       NOT NULL,
    title       VARCHAR(200) NOT NULL,
    content     VARCHAR(500) DEFAULT NULL,
    ref_type    VARCHAR(50)  DEFAULT NULL,
    ref_id      BIGINT       DEFAULT NULL,
    is_read     TINYINT      NOT NULL DEFAULT 0,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_receiver (receiver_id, is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内通知';
