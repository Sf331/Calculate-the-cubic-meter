package com.kelifang.salary.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kelifang.basedata.entity.Clazz;
import com.kelifang.basedata.entity.Course;
import com.kelifang.basedata.entity.Teacher;
import com.kelifang.basedata.service.ClazzService;
import com.kelifang.basedata.service.CourseService;
import com.kelifang.basedata.service.TeacherService;
import com.kelifang.salary.entity.WorkhourRecord;
import com.kelifang.salary.mapper.WorkhourRecordMapper;
import com.kelifang.salary.vo.WorkhourView;
import com.kelifang.schedule.entity.Schedule;
import com.kelifang.schedule.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkhourService extends ServiceImpl<WorkhourRecordMapper, WorkhourRecord> {

    private final ScheduleService scheduleService;
    private final ClazzService clazzService;
    private final CourseService courseService;
    private final TeacherService teacherService;

    /**
     * 记这节课的工时。
     *
     * 一节课一条：金额 = 固定课时单价 × 1 个教学课时。按节算而不是按学生算 ——
     * 5 个学生签到是上了同一节课，不能给老师算 5 份钱。
     *
     * upsert：一节课分两批点名（先点 3 个，下课前补点 2 个）时更新出勤人数，
     * 不会留下第二条工时。
     */
    public WorkhourRecord record(Long teacherId, Long scheduleId, Long attendanceId,
                                 LocalDate workDate, int studentCount, BigDecimal rate) {
        WorkhourRecord row = getOne(Wrappers.<WorkhourRecord>lambdaQuery()
                .eq(WorkhourRecord::getScheduleId, scheduleId));

        if (row == null) {
            row = new WorkhourRecord();
            row.setScheduleId(scheduleId);
        }
        row.setTeacherId(teacherId);
        row.setAttendanceId(attendanceId);
        row.setWorkDate(workDate);
        row.setStudentCount(studentCount);
        row.setRate(rate);
        row.setAmount(rate);
        saveOrUpdate(row);
        return row;
    }

    /** 原始工时记录。报表按教师、按月聚合时用。 */
    public List<WorkhourRecord> between(LocalDate from, LocalDate to) {
        var query = Wrappers.<WorkhourRecord>lambdaQuery()
                .orderByAsc(WorkhourRecord::getWorkDate)
                .orderByAsc(WorkhourRecord::getId);
        if (from != null) {
            query.ge(WorkhourRecord::getWorkDate, from);
        }
        if (to != null) {
            query.le(WorkhourRecord::getWorkDate, to);
        }
        return list(query);
    }

    /** 工时明细行。teacherId 传 null 表示不筛（校长/教务看全部）。 */
    public List<WorkhourView> listByTeacher(Long teacherId, LocalDate from, LocalDate to) {
        var query = Wrappers.<WorkhourRecord>lambdaQuery()
                .orderByAsc(WorkhourRecord::getWorkDate)
                .orderByAsc(WorkhourRecord::getId);
        if (teacherId != null) {
            query.eq(WorkhourRecord::getTeacherId, teacherId);
        }
        if (from != null) {
            query.ge(WorkhourRecord::getWorkDate, from);
        }
        if (to != null) {
            query.le(WorkhourRecord::getWorkDate, to);
        }
        return toViews(list(query));
    }

    public List<WorkhourView> toViews(List<WorkhourRecord> rows) {
        if (rows.isEmpty()) {
            return List.of();
        }

        Map<Long, Schedule> schedules = byId(scheduleService.listByIds(
                rows.stream().map(WorkhourRecord::getScheduleId).distinct().toList()), Schedule::getId);
        Map<Long, Clazz> classes = byId(clazzService.listByIds(schedules.values().stream()
                        .map(Schedule::getClassId).distinct().toList()), Clazz::getId);
        List<Long> courseIds = classes.values().stream()
                .map(Clazz::getCourseId).filter(Objects::nonNull).distinct().toList();
        Map<Long, Course> courses = courseIds.isEmpty()
                ? Map.of()
                : byId(courseService.listByIds(courseIds), Course::getId);
        Map<Long, Teacher> teachers = byId(teacherService.listByIds(
                rows.stream().map(WorkhourRecord::getTeacherId).distinct().toList()), Teacher::getId);

        return rows.stream().map(row -> {
            Schedule schedule = schedules.get(row.getScheduleId());
            Clazz clazz = schedule == null ? null : classes.get(schedule.getClassId());
            Course course = clazz == null ? null : courses.get(clazz.getCourseId());
            Teacher teacher = teachers.get(row.getTeacherId());

            return new WorkhourView(
                    row.getId(),
                    row.getTeacherId(),
                    teacher == null ? null : teacher.getName(),
                    row.getScheduleId(),
                    clazz == null ? null : clazz.getName(),
                    course == null ? null : course.getName(),
                    row.getWorkDate(),
                    row.getStudentCount(),
                    row.getRate(),
                    row.getAmount(),
                    row.getAttendanceId());
        }).toList();
    }

    private static <T> Map<Long, T> byId(List<T> list, Function<T, Long> idOf) {
        return list.stream().collect(Collectors.toMap(idOf, Function.identity()));
    }
}
