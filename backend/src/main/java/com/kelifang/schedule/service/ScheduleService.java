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
import com.kelifang.schedule.engine.GreedyScheduler;
import com.kelifang.schedule.engine.ScheduleContext;
import com.kelifang.schedule.entity.Schedule;
import com.kelifang.schedule.mapper.ScheduleMapper;
import com.kelifang.schedule.vo.GenerateResult;
import com.kelifang.schedule.vo.ScheduleView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
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
