package com.kelifang.attendance.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kelifang.attendance.AttendanceRule;
import com.kelifang.attendance.dto.AttendanceItem;
import com.kelifang.attendance.entity.Attendance;
import com.kelifang.attendance.entity.LessonAccount;
import com.kelifang.attendance.mapper.AttendanceMapper;
import com.kelifang.attendance.vo.ConsumeResult;
import com.kelifang.attendance.vo.SessionStudentView;
import com.kelifang.attendance.vo.SessionView;
import com.kelifang.basedata.entity.Clazz;
import com.kelifang.basedata.entity.Course;
import com.kelifang.basedata.entity.Student;
import com.kelifang.basedata.entity.Teacher;
import com.kelifang.basedata.service.ClazzService;
import com.kelifang.basedata.service.CourseService;
import com.kelifang.basedata.service.TeacherService;
import com.kelifang.common.BizException;
import com.kelifang.common.UserContext;
import com.kelifang.finance.service.FundService;
import com.kelifang.salary.service.WorkhourService;
import com.kelifang.schedule.entity.Schedule;
import com.kelifang.schedule.service.ScheduleService;
import com.kelifang.schedule.vo.ScheduleView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttendanceService extends ServiceImpl<AttendanceMapper, Attendance> {

    private final ScheduleService scheduleService;
    private final ClazzService clazzService;
    private final CourseService courseService;
    private final TeacherService teacherService;
    private final LessonAccountService lessonAccountService;
    private final WorkhourService workhourService;
    private final FundService fundService;

    /**
     * 签到管理页的课次列表。
     * 教师只拿得到自己的课，教务和校长拿全部 —— 过滤在后端，前端传什么都改不了这个范围。
     */
    public List<ScheduleView> lessons(LocalDate from, LocalDate to) {
        Long teacherId = null;
        if ("TEACHER".equals(UserContext.role())) {
            Teacher teacher = teacherService.byUserId(UserContext.userId());
            if (teacher == null) {
                return List.of();
            }
            teacherId = teacher.getId();
        }
        return scheduleService.list(from, to, null, teacherId, null, null);
    }

    /** 点名页：这节课的信息 + 全班名单（含已点状态和剩余课时）。 */
    public SessionView session(Long scheduleId) {
        Schedule schedule = requireSchedule(scheduleId);
        requireCanPoint(schedule);
        Clazz clazz = clazzService.getById(schedule.getClassId());
        Course course = clazz == null ? null : courseService.getById(clazz.getCourseId());
        Teacher teacher = teacherService.getById(schedule.getTeacherId());

        List<Student> roster = clazz == null ? List.of() : clazzService.students(clazz.getId());

        Map<Long, Attendance> signed = list(Wrappers.<Attendance>lambdaQuery()
                .eq(Attendance::getScheduleId, scheduleId))
                .stream()
                .collect(Collectors.toMap(Attendance::getStudentId, Function.identity()));

        // 只有这门课的账户才是这次要扣的那个，同一个学生别的课的余额不掺进来
        List<LessonAccount> accounts = course == null ? List.of()
                : lessonAccountService.accountsOfStudents(
                        roster.stream().map(Student::getId).toList())
                .stream()
                .filter(account -> course.getId().equals(account.getCourseId()))
                .toList();

        Map<Long, LessonAccount> accountByStudent = accounts.stream()
                .collect(Collectors.toMap(LessonAccount::getStudentId, Function.identity()));
        Map<Long, BigDecimal> unitPrices = lessonAccountService.unitPrices(accounts);

        List<SessionStudentView> students = roster.stream().map(student -> {
            Attendance row = signed.get(student.getId());
            LessonAccount account = accountByStudent.get(student.getId());
            return new SessionStudentView(
                    student.getId(),
                    student.getName(),
                    row == null ? null : row.getStatus(),
                    row == null ? null : row.getConsumedHours(),
                    account == null ? null : account.getRemainingHours(),
                    account == null ? null : unitPrices.get(account.getId()));
        }).toList();

        return new SessionView(
                schedule.getId(),
                schedule.getClassId(),
                clazz == null ? null : clazz.getName(),
                course == null ? null : course.getId(),
                course == null ? null : course.getName(),
                schedule.getTeacherId(),
                teacher == null ? null : teacher.getName(),
                schedule.getLessonDate(),
                schedule.getStartTime(),
                schedule.getEndTime(),
                schedule.getStatus(),
                students);
    }

    /**
     * 一次点名，四处同时变。整个 demo 的心脏，就这么四个动作顺序调下来：
     *
     *   1. 写 attendance
     *   2. 扣课时（lesson_transaction + 账户余额）
     *   3. 记教师工时（workhour_record）
     *   4. 确认收入（冲预收 OUT + 记收入 IN）
     *
     * 四步同一事务，任一步失败整体回滚 —— 不允许出现"课时扣了但收入没记"这种半截数据。
     */
    @Transactional
    public ConsumeResult consume(Long scheduleId, List<AttendanceItem> items) {
        if (items == null || items.isEmpty()) {
            throw BizException.badRequest("没有要提交的签到");
        }

        Schedule schedule = requireSchedule(scheduleId);
        requireCanPoint(schedule);

        Clazz clazz = clazzService.getById(schedule.getClassId());
        if (clazz == null) {
            throw BizException.badRequest("这节课所属的班级已被删除");
        }
        Course course = courseService.getById(clazz.getCourseId());
        if (course == null) {
            throw BizException.badRequest("班级关联的课程不存在，无法核销课时");
        }

        Map<Long, Student> roster = clazzService.students(clazz.getId()).stream()
                .collect(Collectors.toMap(Student::getId, Function.identity()));

        BigDecimal consumedHours = BigDecimal.ZERO;
        BigDecimal confirmedAmount = BigDecimal.ZERO;

        for (AttendanceItem item : items) {
            Student student = roster.get(item.getStudentId());
            if (student == null) {
                throw BizException.badRequest("点名的学生不在这个班的名单里");
            }
            // 防重复核销：唯一键是最后一道，这里先给出人能看懂的原因
            if (alreadySigned(scheduleId, student.getId())) {
                throw BizException.badRequest(student.getName() + "这节课已经点过名了，不能重复核销");
            }

            BigDecimal hours = AttendanceRule.hoursOf(item.getStatus());

            Attendance row = new Attendance();
            row.setScheduleId(scheduleId);
            row.setStudentId(student.getId());
            row.setStatus(item.getStatus());
            row.setSignMethod("TEACHER");
            row.setSignTime(LocalDateTime.now());
            row.setConsumedHours(hours);
            save(row);

            LessonAccount account = lessonAccountService.accountOf(
                    student.getId(), course.getId(), student.getName());
            BigDecimal unitPrice = lessonAccountService.unitPrice(account,
                    fundService.paidTotal(account.getId()));

            lessonAccountService.consume(account, hours, row.getId(), "签到消耗：" + course.getName());
            confirmedAmount = confirmedAmount.add(fundService.confirmRevenue(
                    student.getId(), unitPrice, hours, row.getId(), schedule.getLessonDate()));

            consumedHours = consumedHours.add(hours);
        }

        // 工时不按人数算：这节课有人来就给老师记 1 个教学课时。
        // 出勤人数每次都从 attendance 现算，一节课分两批点名也不会重复计酬。
        BigDecimal workhourAmount = BigDecimal.ZERO;
        List<Attendance> taught = list(Wrappers.<Attendance>lambdaQuery()
                .eq(Attendance::getScheduleId, scheduleId)
                .in(Attendance::getStatus, AttendanceRule.TAUGHT)
                .orderByAsc(Attendance::getId));

        if (!taught.isEmpty()) {
            Teacher teacher = teacherService.getById(schedule.getTeacherId());
            BigDecimal rate = teacher == null || teacher.getHourlyRate() == null
                    ? BigDecimal.ZERO
                    : teacher.getHourlyRate();

            workhourAmount = workhourService.record(schedule.getTeacherId(), scheduleId,
                    taught.get(0).getId(), schedule.getLessonDate(), taught.size(), rate).getAmount();
        }

        schedule.setStatus("DONE");
        scheduleService.updateById(schedule);

        return new ConsumeResult(scheduleId, items.size(), consumedHours, confirmedAmount, workhourAmount);
    }

    private boolean alreadySigned(Long scheduleId, Long studentId) {
        return count(Wrappers.<Attendance>lambdaQuery()
                .eq(Attendance::getScheduleId, scheduleId)
                .eq(Attendance::getStudentId, studentId)) > 0;
    }

    private Schedule requireSchedule(Long scheduleId) {
        Schedule schedule = scheduleService.getById(scheduleId);
        if (schedule == null) {
            throw BizException.notFound("这节课不存在");
        }
        if ("CANCELLED".equals(schedule.getStatus())) {
            throw BizException.badRequest("这节课已取消，不能点名");
        }
        return schedule;
    }

    /**
     * 后端权限校验，不是靠前端藏菜单。
     * 教师只能给本人的课点名，教务和校长可以代点，学生和家长不行。
     */
    private void requireCanPoint(Schedule schedule) {
        String role = UserContext.role();
        if ("PRINCIPAL".equals(role) || "ACADEMIC".equals(role)) {
            return;
        }
        if ("TEACHER".equals(role)) {
            Teacher teacher = teacherService.byUserId(UserContext.userId());
            if (teacher == null || !teacher.getId().equals(schedule.getTeacherId())) {
                throw BizException.forbidden("只能给本人的课点名");
            }
            return;
        }
        throw BizException.forbidden("当前角色不能点名");
    }
}
