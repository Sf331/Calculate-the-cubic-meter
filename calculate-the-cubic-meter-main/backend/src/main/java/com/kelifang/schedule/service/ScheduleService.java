package com.kelifang.schedule.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kelifang.basedata.entity.Classroom;
import com.kelifang.basedata.entity.Clazz;
import com.kelifang.basedata.entity.Course;
import com.kelifang.basedata.entity.Student;
import com.kelifang.basedata.entity.Teacher;
import com.kelifang.basedata.service.ClazzService;
import com.kelifang.basedata.service.ClassroomService;
import com.kelifang.basedata.service.CourseService;
import com.kelifang.basedata.service.StudentService;
import com.kelifang.basedata.service.TeacherService;
import com.kelifang.common.BizException;
import com.kelifang.schedule.engine.Booking;
import com.kelifang.schedule.engine.ClassTask;
import com.kelifang.schedule.engine.ConstraintValidator;
import com.kelifang.schedule.engine.GreedyScheduler;
import com.kelifang.schedule.engine.IncrementalRescheduler;
import com.kelifang.schedule.engine.Placement;
import com.kelifang.schedule.engine.ScheduleContext;
import com.kelifang.schedule.entity.Schedule;
import com.kelifang.schedule.mapper.ScheduleMapper;
import com.kelifang.schedule.vo.GenerateResult;
import com.kelifang.schedule.vo.RescheduleMove;
import com.kelifang.schedule.vo.ReschedulePlan;
import com.kelifang.schedule.vo.ScheduleView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScheduleService extends ServiceImpl<ScheduleMapper, Schedule> {

    private static final int MAX_WEEKS = 30;

    private final ClazzService clazzService;
    private final CourseService courseService;
    private final TeacherService teacherService;
    private final StudentService studentService;
    private final ClassroomService classroomService;

    /**
     * 生成课表。语义是"重新出初稿"：先清空所有未锁定的课表，再整表重排。
     * locked = 1 的行不参与求解，但作为占用基准，重排时会被避开。
     */
    @Transactional
    public GenerateResult generate(LocalDate startDate, Integer weeks) {
        if (startDate == null) {
            throw BizException.badRequest("必须指定起始日期");
        }
        int weekCount = (weeks == null || weeks <= 0) ? 1 : Math.min(weeks, MAX_WEEKS);

        ScheduleContext context = buildContext();
        GreedyScheduler.Result result = new GreedyScheduler(context).run(startDate, weekCount);

        remove(Wrappers.<Schedule>lambdaQuery().eq(Schedule::getLocked, 0));

        Map<Long, Clazz> classById = context.tasks().stream()
                .collect(Collectors.toMap(t -> t.clazz().getId(), ClassTask::clazz));

        List<Schedule> rows = result.placed().stream()
                .map(booking -> toEntity(booking, classById.get(booking.classId())))
                .toList();
        saveBatch(rows);

        return GenerateResult.of(rows.size(), result.conflicts());
    }

    public List<ScheduleView> list(LocalDate from, LocalDate to, Long classId,
                                   Long teacherId, Long classroomId, Long studentId) {
        var query = Wrappers.<Schedule>lambdaQuery().orderByAsc(Schedule::getLessonDate)
                .orderByAsc(Schedule::getStartTime);

        if (from != null) {
            query.ge(Schedule::getLessonDate, from);
        }
        if (to != null) {
            query.le(Schedule::getLessonDate, to);
        }
        if (classId != null) {
            query.eq(Schedule::getClassId, classId);
        }
        if (teacherId != null) {
            query.eq(Schedule::getTeacherId, teacherId);
        }
        if (classroomId != null) {
            query.eq(Schedule::getClassroomId, classroomId);
        }
        if (studentId != null) {
            List<Long> classIds = clazzService.classIdsOfStudent(studentId);
            if (classIds.isEmpty()) {
                return List.of();
            }
            query.in(Schedule::getClassId, classIds);
        }

        return toViews(list(query));
    }

    @Transactional
    public void clear() {
        remove(Wrappers.<Schedule>lambdaQuery().eq(Schedule::getLocked, 0));
    }

    /**
     * 某个日期区间内的课次 id。报表要按"课次日期"归属数据时从这儿进来
     * —— 签到、收入都该算在课上的那天，不是算在老师点鼠标的那天。
     */
    public List<Long> idsBetween(LocalDate from, LocalDate to) {
        return list(Wrappers.<Schedule>lambdaQuery()
                .select(Schedule::getId)
                .ge(Schedule::getLessonDate, from)
                .le(Schedule::getLessonDate, to))
                .stream()
                .map(Schedule::getId)
                .toList();
    }

    /**
     * 手动把一节课挪到新时段。
     *
     * 复用排课引擎的 ConstraintValidator，不另写一套规则 —— 否则"自动排出来的课合法、
     * 手动挪一下就非法"这种不一致迟早出问题。
     *
     * 返回被违反的约束列表：空列表表示挪成功，非空表示被拒且原因是什么。
     * 撞到别的课就拒绝，不会自动调整别的课 —— 那走 {@link #reschedule}。
     */
    @Transactional
    public List<String> move(Long scheduleId, LocalDate date, LocalTime start, Long classroomId) {
        if (date == null || start == null) {
            throw BizException.badRequest("必须指定日期和开始时间");
        }

        Schedule row = getById(scheduleId);
        if (row == null) {
            throw BizException.notFound("这节课不存在");
        }
        if (Integer.valueOf(1).equals(row.getLocked())) {
            throw BizException.badRequest("已锁定的课表不能调整");
        }

        Clazz clazz = clazzService.getById(row.getClassId());
        if (clazz == null) {
            throw BizException.badRequest("这节课所属的班级已被删除");
        }

        Map<Long, List<Student>> roster = clazzService.studentsByClass(
                clazzService.list().stream().map(Clazz::getId).toList());
        ClassTask task = buildTask(clazz, roster);

        Long roomId = classroomId != null ? classroomId : row.getClassroomId();
        LocalTime end = start.plusMinutes(durationMinutes(task));

        List<Classroom> classrooms = classroomService.list();
        ScheduleContext context = new ScheduleContext(
                List.of(task),
                byId(classrooms, Classroom::getId),
                classrooms,
                teacherService.windowsByTeacher(List.of(clazz.getTeacherId())),
                studentService.constraintsByStudent(List.copyOf(task.studentIds())),
                otherBookings(scheduleId, roster));

        List<String> reasons = new ConstraintValidator(context)
                .validate(task, roomId, date, start, end);

        if (reasons.isEmpty()) {
            row.setLessonDate(date);
            row.setStartTime(start);
            row.setEndTime(end);
            row.setClassroomId(roomId);
            updateById(row);
        }
        return reasons;
    }

    /** 除自己以外的所有课表，作为挪课时的占用基准。 */
    private List<Booking> otherBookings(Long excludeId, Map<Long, List<Student>> roster) {
        return list(Wrappers.<Schedule>lambdaQuery().ne(Schedule::getId, excludeId)).stream()
                .map(row -> new Booking(
                        row.getClassId(),
                        row.getTeacherId(),
                        row.getClassroomId(),
                        row.getLessonDate(),
                        row.getStartTime(),
                        row.getEndTime(),
                        roster.getOrDefault(row.getClassId(), List.of()).stream()
                                .map(Student::getId).collect(Collectors.toSet())))
                .toList();
    }

    /**
     * 增量重排：把一节课挪到新时段，撞到的课由引擎自动另找位置。
     *
     * dryRun = true 只出方案不落库，前端先把影响面给教务看；确认后再传 false 落库。
     * 两次调用跑的是同一套确定性算法，所以方案一致 —— 不需要把方案回传给服务端。
     */
    @Transactional
    public ReschedulePlan reschedule(Long scheduleId, LocalDate date, LocalTime start,
                                     Long classroomId, boolean dryRun) {
        if (date == null || start == null) {
            throw BizException.badRequest("必须指定日期和开始时间");
        }

        Schedule target = getById(scheduleId);
        if (target == null) {
            throw BizException.notFound("这节课不存在");
        }
        if (Integer.valueOf(1).equals(target.getLocked())) {
            throw BizException.badRequest("已锁定的课表不能调整");
        }

        ScheduleContext context = buildContext();
        Map<Long, ClassTask> taskByClass = context.tasks().stream()
                .collect(Collectors.toMap(t -> t.clazz().getId(), Function.identity()));

        IncrementalRescheduler.Result result =
                new IncrementalRescheduler(context, allPlacements(taskByClass))
                        .resolve(scheduleId, date, start, classroomId);

        ReschedulePlan plan = toPlan(result, context, taskByClass);
        if (plan.feasible() && !dryRun) {
            applyMoves(result.moves());
        }
        return plan;
    }

    private List<Placement> allPlacements(Map<Long, ClassTask> taskByClass) {
        return list().stream().map(row -> {
            ClassTask task = taskByClass.get(row.getClassId());
            return new Placement(
                    row.getId(),
                    row.getClassId(),
                    row.getTeacherId(),
                    row.getClassroomId(),
                    row.getLessonDate(),
                    row.getStartTime(),
                    row.getEndTime(),
                    task == null ? Set.of() : task.studentIds(),
                    Integer.valueOf(1).equals(row.getLocked()));
        }).toList();
    }

    private void applyMoves(List<IncrementalRescheduler.Move> moves) {
        for (IncrementalRescheduler.Move move : moves) {
            Schedule row = getById(move.to().scheduleId());
            if (row == null) {
                continue;
            }
            row.setLessonDate(move.to().date());
            row.setStartTime(move.to().start());
            row.setEndTime(move.to().end());
            row.setClassroomId(move.to().classroomId());
            updateById(row);
        }
    }

    /** 把引擎的方案翻成带名字的展示结构，顺带算影响面。 */
    private ReschedulePlan toPlan(IncrementalRescheduler.Result result,
                                  ScheduleContext context, Map<Long, ClassTask> taskByClass) {
        if (!result.feasible()) {
            return new ReschedulePlan(false, List.of(), List.of(), List.of(), 0, result.reasons());
        }

        List<RescheduleMove> moves = new ArrayList<>();
        Set<String> classNames = new LinkedHashSet<>();
        Set<String> teacherNames = new LinkedHashSet<>();
        Set<Long> studentIds = new HashSet<>();

        for (IncrementalRescheduler.Move move : result.moves()) {
            Placement to = move.to();
            ClassTask task = taskByClass.get(to.classId());

            String className = task == null ? "班级" + to.classId() : task.clazz().getName();
            String courseName = task == null ? null : task.course().getName();
            String teacherName = task == null ? null : task.teacher().getName();

            classNames.add(className);
            if (teacherName != null) {
                teacherNames.add(teacherName);
            }
            studentIds.addAll(to.studentIds());

            moves.add(new RescheduleMove(
                    to.scheduleId(), className, courseName, teacherName,
                    move.from().date(), move.from().start(), move.from().end(),
                    roomName(context, move.from().classroomId()),
                    to.date(), to.start(), to.end(),
                    roomName(context, to.classroomId())));
        }

        return new ReschedulePlan(true, moves,
                List.copyOf(classNames), List.copyOf(teacherNames), studentIds.size(), List.of());
    }

    private static String roomName(ScheduleContext context, Long classroomId) {
        Classroom classroom = context.classrooms().get(classroomId);
        return classroom == null ? null : classroom.getName();
    }

    private ClassTask buildTask(Clazz clazz, Map<Long, List<Student>> roster) {
        Course course = courseService.getById(clazz.getCourseId());
        Teacher teacher = teacherService.getById(clazz.getTeacherId());
        if (course == null || teacher == null) {
            throw BizException.badRequest("班级关联的课程或教师不存在，无法校验");
        }
        return new ClassTask(clazz, course, teacher,
                roster.getOrDefault(clazz.getId(), List.of()));
    }

    private static int durationMinutes(ClassTask task) {
        Integer minutes = task.course().getDurationMinutes();
        return (minutes == null || minutes <= 0) ? 45 : minutes;
    }

    // ---- 组装引擎输入 ----

    private ScheduleContext buildContext() {
        List<Clazz> classes = clazzService.list();
        if (classes.isEmpty()) {
            throw BizException.badRequest("还没有班级，无法排课");
        }

        Map<Long, List<Student>> roster = clazzService.studentsByClass(
                classes.stream().map(Clazz::getId).toList());

        Map<Long, Course> courses = byId(courseService.listByIds(
                distinctIds(classes, Clazz::getCourseId)), Course::getId);
        Map<Long, Teacher> teachers = byId(teacherService.listByIds(
                distinctIds(classes, Clazz::getTeacherId)), Teacher::getId);

        List<ClassTask> tasks = new ArrayList<>();
        for (Clazz clazz : classes) {
            Course course = courses.get(clazz.getCourseId());
            Teacher teacher = teachers.get(clazz.getTeacherId());
            if (course == null || teacher == null) {
                // 数据不全的班级跳过，比如课程或教师已被删除
                continue;
            }
            tasks.add(new ClassTask(clazz, course, teacher,
                    roster.getOrDefault(clazz.getId(), List.of())));
        }
        if (tasks.isEmpty()) {
            throw BizException.badRequest("没有可排课的班级：请检查班级关联的课程和教师是否还存在");
        }

        List<Classroom> classrooms = classroomService.list();

        List<Long> studentIds = tasks.stream()
                .flatMap(t -> t.studentIds().stream())
                .distinct()
                .toList();

        return new ScheduleContext(
                tasks,
                byId(classrooms, Classroom::getId),
                classrooms,
                teacherService.windowsByTeacher(distinctIds(classes, Clazz::getTeacherId)),
                studentService.constraintsByStudent(studentIds),
                lockedBookings(tasks));
    }

    /** locked = 1 的既有课表。不重排，但占用的时段不能再被别人用。 */
    private List<Booking> lockedBookings(List<ClassTask> tasks) {
        Map<Long, ClassTask> byClassId = tasks.stream()
                .collect(Collectors.toMap(t -> t.clazz().getId(), Function.identity()));

        return list(Wrappers.<Schedule>lambdaQuery().eq(Schedule::getLocked, 1)).stream()
                .map(row -> {
                    ClassTask task = byClassId.get(row.getClassId());
                    return new Booking(
                            row.getClassId(),
                            row.getTeacherId(),
                            row.getClassroomId(),
                            row.getLessonDate(),
                            row.getStartTime(),
                            row.getEndTime(),
                            task == null ? Set.of() : task.studentIds());
                })
                .toList();
    }

    // ---- 转换 ----

    private Schedule toEntity(Booking booking, Clazz clazz) {
        Schedule row = new Schedule();
        row.setClassId(booking.classId());
        row.setTeacherId(booking.teacherId());
        row.setClassroomId(booking.classroomId());
        row.setCampusId(clazz == null ? null : clazz.getCampusId());
        row.setLessonDate(booking.date());
        row.setStartTime(booking.start());
        row.setEndTime(booking.end());
        row.setStatus("PLANNED");
        row.setLocked(0);
        return row;
    }

    private List<ScheduleView> toViews(List<Schedule> rows) {
        if (rows.isEmpty()) {
            return List.of();
        }

        Map<Long, Clazz> classes = byId(clazzService.listByIds(
                rows.stream().map(Schedule::getClassId).distinct().toList()), Clazz::getId);
        Map<Long, Teacher> teachers = byId(teacherService.listByIds(
                rows.stream().map(Schedule::getTeacherId).distinct().toList()), Teacher::getId);
        Map<Long, Classroom> classrooms = byId(classroomService.listByIds(
                rows.stream().map(Schedule::getClassroomId).filter(Objects::nonNull)
                        .distinct().toList()), Classroom::getId);

        List<Long> courseIds = classes.values().stream().map(Clazz::getCourseId)
                .filter(Objects::nonNull).distinct().toList();
        Map<Long, Course> courses = courseIds.isEmpty()
                ? Map.of()
                : byId(courseService.listByIds(courseIds), Course::getId);

        return rows.stream().map(row -> {
            Clazz clazz = classes.get(row.getClassId());
            Teacher teacher = teachers.get(row.getTeacherId());
            Classroom classroom = classrooms.get(row.getClassroomId());
            Course course = clazz == null ? null : courses.get(clazz.getCourseId());

            return new ScheduleView(
                    row.getId(),
                    row.getClassId(), clazz == null ? null : clazz.getName(),
                    row.getTeacherId(), teacher == null ? null : teacher.getName(),
                    row.getClassroomId(), classroom == null ? null : classroom.getName(),
                    course == null ? null : course.getId(),
                    course == null ? null : course.getName(),
                    course == null ? null : course.getSubject(),
                    row.getLessonDate(), row.getStartTime(), row.getEndTime(),
                    row.getLocked(), row.getStatus());
        }).toList();
    }

    // ---- 小工具 ----

    private static <T> Map<Long, T> byId(List<T> list, Function<T, Long> idOf) {
        return list.stream().collect(Collectors.toMap(idOf, Function.identity()));
    }

    private static List<Long> distinctIds(List<Clazz> classes, Function<Clazz, Long> idOf) {
        return classes.stream().map(idOf).filter(Objects::nonNull).distinct().toList();
    }
}
