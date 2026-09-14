-- 课立方 demo 种子数据
--
-- 每次启动随 schema.sql 一起重建，所以只需要保证"系统刚起来就能登录、能演示"。
-- 完整的演示数据（300 学生 / 15 教师 / 8 班级 / 一学期课表 / 历史流水）在演示打磨阶段补。
--
-- 五类测试账号密码统一 123456，登录页提供快捷登录按钮，演示时不用手敲。

INSERT INTO campus (id, name, address) VALUES
  (1, '总校区', '示例市示例区示例路 1 号'),
  (2, '城东校区', '示例市城东区示例路 2 号');

INSERT INTO sys_user (id, username, password, real_name, role, phone) VALUES
  (1, 'principal', '123456', '王校长', 'PRINCIPAL', '13800000001'),
  (2, 'academic',  '123456', '张教务', 'ACADEMIC',  '13800000002'),
  (3, 'teacher01', '123456', '李老师', 'TEACHER',   '13800000003'),
  (4, 'student01', '123456', '小明',   'STUDENT',   '13800000004'),
  (5, 'parent01',  '123456', '小明妈妈', 'PARENT',  '13800000005');

-- 让教师 / 学生 / 家长三个账号先有对应的业务档案，登录后才能看到自己的数据。
INSERT INTO teacher (id, user_id, name, subject, campus_id, salary_type, base_salary) VALUES
  (1, 3, '李老师', '数学', 1, 'BASE_HOURLY', 2000.00);

INSERT INTO student (id, user_id, parent_user_id, name, grade, campus_id) VALUES
  (1, 4, 5, '小明', '初一', 1);
