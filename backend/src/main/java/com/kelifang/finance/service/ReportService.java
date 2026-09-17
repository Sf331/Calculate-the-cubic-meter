package com.kelifang.finance.service;

import com.kelifang.attendance.entity.Attendance;
import com.kelifang.attendance.entity.LessonAccount;
import com.kelifang.attendance.entity.LessonTransaction;
import com.kelifang.attendance.service.AttendanceService;
import com.kelifang.attendance.service.LessonAccountService;
import com.kelifang.basedata.entity.Clazz;
import com.kelifang.basedata.entity.Course;
import com.kelifang.basedata.entity.Student;
import com.kelifang.basedata.service.ClazzService;
import com.kelifang.basedata.service.CourseService;
import com.kelifang.basedata.service.StudentService;
import com.kelifang.finance.entity.FundTransaction;
import com.kelifang.finance.vo.ClassProfitRow;
import com.kelifang.finance.vo.LessonCarryRow;
import com.kelifang.finance.vo.PreReceiveRow;
import com.kelifang.finance.vo.TeacherCostRow;
import com.kelifang.salary.entity.WorkhourRecord;
import com.kelifang.salary.service.WorkhourService;
import com.kelifang.salary.vo.WorkhourView;
import com.kelifang.schedule.entity.Schedule;
import com.kelifang.schedule.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 财会报表。五张表全部从已有的流水聚合出来，不另建台账。
 *
 * 跨模块一律走对方的 Service（不直接查别人的 Mapper），所以这个类依赖有点多 ——
 * 但它是"只读聚合"的定位，不下沉业务规则。
 */
@Service
@RequiredArgsConstructor
public class ReportService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final FundService fundService;
    private final AttendanceService attendanceService;
    private final LessonAccountService lessonAccountService;
    private final WorkhourService workhourService;
    private final ScheduleService scheduleService;
    private final ClazzService clazzService;
    private final CourseService courseService;
    private final StudentService studentService;

    /**
     * 预收台账：收了多少钱、确认了多少收入、还挂着多少预收。
     *
     * 台账是**累计数**，所以只认截止日不认起始日 —— "期末余额"本来就是算到某天为止的。
     */
    public List<PreReceiveRow> preReceive(YearMonth month) {
        LocalDate until = month.atEndOfMonth();
        List<FundTransaction> rows = fundService.query(null, null, until, "PRE_RECEIVE");

        Map<Long, BigDecimal[]> byStudent = new LinkedHashMap<>();
        for (FundTransaction row : rows) {
            if (row.getStudentId() == null) {
                continue;
            }
            BigDecimal[] acc = byStudent.computeIfAbsent(row.getStudentId(),
                    key -> new BigDecimal[]{ZERO, ZERO});
            // 收进来的是预收，冲出去的就是已经消耗掉的那部分
            if ("IN".equals(row.getDirection())) {
                acc[0] = acc[0].add(row.getAmount());
            } else {
                acc[1] = acc[1].add(row.getAmount());
            }
        }

        Map<Long, String> names = studentNames(byStudent.keySet());
        return byStudent.entrySet().stream()
                .map(entry -> new PreReceiveRow(
                        entry.getKey(),
                        names.get(entry.getKey()),
                        entry.getValue()[0],
                        entry.getValue()[1],
                        entry.getValue()[0].subtract(entry.getValue()[1])))
                .sorted(Comparator.comparing(PreReceiveRow::studentId))
                .toList();
    }

    /**
     * 课时结转表：某个账户在这个月的 期初 + 充值 − 消耗 = 期末。
     * 这个月没有任何变动的账户不列出来，否则 48 个账户全是 0 的行。
     */
    public List<LessonCarryRow> lessonCarry(YearMonth month) {
        LocalDate monthStart = month.atDay(1);
        List<LessonTransaction> all = lessonAccountService.transactionsBetween(
                LocalDate.of(1970, 1, 1), month.atEndOfMonth());

        Map<Long, List<LessonTransaction>> byAccount = all.stream()
                .collect(Collectors.groupingBy(LessonTransaction::getAccountId));
        if (byAccount.isEmpty()) {
            return List.of();
        }

        Map<Long, LessonAccount> accounts = lessonAccountService
                .listByIds(byAccount.keySet()).stream()
                .collect(Collectors.toMap(LessonAccount::getId, Function.identity()));
        Map<Long, String> studentNames = studentNames(accounts.values().stream()
                .map(LessonAccount::getStudentId).collect(Collectors.toSet()));
        Map<Long, Course> courses = courseService.listByIds(accounts.values().stream()
                        .map(LessonAccount::getCourseId).distinct().toList()).stream()
                .collect(Collectors.toMap(Course::getId, Function.identity()));

        List<LessonCarryRow> result = new ArrayList<>();
        for (Map.Entry<Long, List<LessonTransaction>> entry : byAccount.entrySet()) {
            LessonAccount account = accounts.get(entry.getKey());
            if (account == null) {
                continue;
            }

            BigDecimal opening = ZERO;
            BigDecimal recharged = ZERO;
            BigDecimal consumed = ZERO;
            for (LessonTransaction tx : entry.getValue()) {
                if (tx.getCreatedAt().toLocalDate().isBefore(monthStart)) {
                    opening = opening.add(tx.getHours());
                } else if (tx.getHours().signum() >= 0) {
                    recharged = recharged.add(tx.getHours());
                } else {
                    consumed = consumed.add(tx.getHours().negate());
                }
            }
            if (recharged.signum() == 0 && consumed.signum() == 0) {
                continue;
            }

            Course course = courses.get(account.getCourseId());
            result.add(new LessonCarryRow(
                    account.getStudentId(),
                    studentNames.get(account.getStudentId()),
                    account.getCourseId(),
                    course == null ? null : course.getName(),
                    month.toString(),
                    opening,
                    recharged,
                    consumed,
                    opening.add(recharged).subtract(consumed)));
        }
        result.sort(Comparator.comparing(LessonCarryRow::studentId));
        return result;
    }

    /** 教师成本表：这个月每位老师上了几节课、拢共多少钱。 */
    public List<TeacherCostRow> teacherCost(YearMonth month) {
        List<WorkhourView> rows = workhourService.listByTeacher(
                null, month.atDay(1), month.atEndOfMonth());

        Map<Long, List<WorkhourView>> byTeacher = rows.stream()
                .collect(Collectors.groupingBy(WorkhourView::teacherId,
                        LinkedHashMap::new, Collectors.toList()));

        return byTeacher.entrySet().stream()
                .map(entry -> new TeacherCostRow(
                        entry.getKey(),
                        entry.getValue().get(0).teacherName(),
                        entry.getValue().size(),
                        entry.getValue().stream()
                                .mapToInt(row -> row.studentCount() == null ? 0 : row.studentCount())
                                .sum(),
                        entry.getValue().stream()
                                .map(WorkhourView::amount)
                                .reduce(ZERO, BigDecimal::add)))
                .sorted(Comparator.comparing(TeacherCostRow::teacherId))
                .toList();
    }

    /**
     * 班级毛利表：班级确认收入 − 该班教师成本。
     *
     * 收入要三跳才对得上班级：fund_transaction(ref_id) → attendance → schedule → clazz。
     * 一笔收入是由某次签到产生的，签到属于某节课，课属于某个班。
     */
    public List<ClassProfitRow> classProfit(YearMonth month) {
        LocalDate from = month.atDay(1);
        LocalDate to = month.atEndOfMonth();

        Map<Long, BigDecimal> revenueByClass = revenueByClass(from, to);

        Map<Long, BigDecimal> costByClass = new LinkedHashMap<>();
        List<WorkhourRecord> workhours = workhourService.between(from, to);
        Map<Long, Schedule> scheduleById = scheduleService.listByIds(
                        workhours.stream().map(WorkhourRecord::getScheduleId).distinct().toList()).stream()
                .collect(Collectors.toMap(Schedule::getId, Function.identity()));
        for (WorkhourRecord row : workhours) {
            Schedule schedule = scheduleById.get(row.getScheduleId());
            if (schedule == null) {
                continue;
            }
            costByClass.merge(schedule.getClassId(), row.getAmount(), BigDecimal::add);
        }

        List<Long> classIds = new ArrayList<>(revenueByClass.keySet());
        costByClass.keySet().stream().filter(id -> !revenueByClass.containsKey(id)).forEach(classIds::add);
        if (classIds.isEmpty()) {
            return List.of();
        }

        Map<Long, Clazz> classes = clazzService.listByIds(classIds).stream()
                .collect(Collectors.toMap(Clazz::getId, Function.identity()));
        Map<Long, Course> courses = courseService.listByIds(classes.values().stream()
                        .map(Clazz::getCourseId).filter(Objects::nonNull).distinct().toList()).stream()
                .collect(Collectors.toMap(Course::getId, Function.identity()));

        return classIds.stream().map(classId -> {
            Clazz clazz = classes.get(classId);
            Course course = clazz == null ? null : courses.get(clazz.getCourseId());
            BigDecimal revenue = revenueByClass.getOrDefault(classId, ZERO);
            BigDecimal cost = costByClass.getOrDefault(classId, ZERO);
            return new ClassProfitRow(
                    classId,
                    clazz == null ? null : clazz.getName(),
                    course == null ? null : course.getName(),
                    revenue,
                    cost,
                    revenue.subtract(cost));
        }).sorted(Comparator.comparing(ClassProfitRow::classId)).toList();
    }

    private Map<Long, BigDecimal> revenueByClass(LocalDate from, LocalDate to) {
        List<FundTransaction> confirms = fundService.query(null, from, to, "RECEIVE_CONFIRM");
        List<Long> attendanceIds = confirms.stream()
                .map(FundTransaction::getRefId).filter(Objects::nonNull).distinct().toList();
        if (attendanceIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, Long> scheduleIdByAttendance = attendanceService.byIds(attendanceIds).stream()
                .collect(Collectors.toMap(Attendance::getId, Attendance::getScheduleId));
        Map<Long, Schedule> schedules = scheduleService.listByIds(
                        scheduleIdByAttendance.values().stream().distinct().toList()).stream()
                .collect(Collectors.toMap(Schedule::getId, Function.identity()));

        Map<Long, BigDecimal> byClass = new LinkedHashMap<>();
        for (FundTransaction confirm : confirms) {
            Long scheduleId = scheduleIdByAttendance.get(confirm.getRefId());
            Schedule schedule = scheduleId == null ? null : schedules.get(scheduleId);
            if (schedule == null) {
                continue;
            }
            byClass.merge(schedule.getClassId(), confirm.getAmount(), BigDecimal::add);
        }
        return byClass;
    }

    private Map<Long, String> studentNames(Collection<Long> studentIds) {
        if (studentIds.isEmpty()) {
            return Map.of();
        }
        return studentService.listByIds(List.copyOf(studentIds)).stream()
                .collect(Collectors.toMap(Student::getId, Student::getName));
    }
}
