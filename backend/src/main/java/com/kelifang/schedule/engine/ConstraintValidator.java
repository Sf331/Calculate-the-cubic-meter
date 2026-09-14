package com.kelifang.schedule.engine;

import com.kelifang.basedata.entity.Classroom;
import com.kelifang.basedata.entity.Student;
import com.kelifang.basedata.entity.StudentConstraint;
import com.kelifang.basedata.entity.TeacherAvailability;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 硬约束校验。对应计划书表 5-1 的硬约束部分。
 *
 * 刻意返回"所有被违反的约束"而不是只返回第一条 —— 演示时教务最想看到的是
 * "这节课为什么排不进去"，把所有原因一次列出来才有用。
 */
public class ConstraintValidator {

    private static final String TYPE_UNAVAILABLE = "UNAVAILABLE";
    private static final String TYPE_AVAILABLE = "AVAILABLE";

    private final ScheduleContext context;
    private final List<Booking> placed = new ArrayList<>();

    public ConstraintValidator(ScheduleContext context) {
        this.context = context;
        this.placed.addAll(context.locked());
    }

    /** 返回被违反的硬约束。空列表表示这个时段合法。 */
    public List<String> validate(ClassTask task, Long classroomId,
                                 LocalDate date, LocalTime start, LocalTime end) {
        List<String> reasons = new ArrayList<>();
        DayOfWeek weekday = date.getDayOfWeek();
        Set<Long> studentIds = task.studentIds();

        if (!TeachingGrid.withinGrid(weekday, start, end)) {
            // 时段本身就不合法，后面的检查没有意义
            return List.of("超出允许的教学时段");
        }

        checkQualification(task, reasons);
        Classroom classroom = checkClassroom(classroomId, studentIds.size(), reasons);
        checkTeacherWindow(task, weekday, start, end, reasons);
        checkStudentWindows(task, weekday, start, end, reasons);
        checkOccupancy(task, classroomId, classroom, date, start, end, studentIds, reasons);

        // 多条占用冲突可能产生重复文案，去个重
        return reasons.stream().distinct().toList();
    }

    /** 把一节排定的课登记进去，后续校验都会看到它。 */
    public void place(Booking booking) {
        placed.add(booking);
    }

    // ---- 各条硬约束 ----

    /** 教师资质与课程科目匹配。schema 里教师只有一个 subject 字段，所以是单科目匹配。 */
    private void checkQualification(ClassTask task, List<String> reasons) {
        String courseSubject = task.course().getSubject();
        String teacherSubject = task.teacher().getSubject();
        if (courseSubject == null || !courseSubject.equals(teacherSubject)) {
            reasons.add("教师" + task.teacher().getName() + "不具备"
                    + courseSubject + "科目的授课资质");
        }
    }

    /** 教室容量要放得下班级实际人数 */
    private Classroom checkClassroom(Long classroomId, int studentCount, List<String> reasons) {
        Classroom classroom = context.classrooms().get(classroomId);
        if (classroom == null) {
            reasons.add("教室不存在");
            return null;
        }
        // 容量为 0 视为不限制
        Integer capacity = classroom.getCapacity();
        if (capacity != null && capacity > 0 && studentCount > capacity) {
            reasons.add("教室" + classroom.getName() + "容量 " + capacity
                    + " 人，放不下 " + studentCount + " 名学生");
        }
        return classroom;
    }

    /** 教师可用时段。没配过可用时段的教师视为不受限。 */
    private void checkTeacherWindow(ClassTask task, DayOfWeek weekday,
                                    LocalTime start, LocalTime end, List<String> reasons) {
        List<TeacherAvailability> windows = context.teacherWindows().get(task.teacher().getId());
        if (windows == null || windows.isEmpty()) {
            return;
        }
        boolean ok = windows.stream().anyMatch(w ->
                w.getWeekday() == weekday.getValue()
                        && Booking.covers(w.getStartTime(), w.getEndTime(), start, end));
        if (!ok) {
            reasons.add("教师" + task.teacher().getName() + "在该时段不可授课");
        }
    }

    /** 学生时段约束。只配了 UNAVAILABLE 就只挡不可排时段，配了 AVAILABLE 则必须在可用时段内。 */
    private void checkStudentWindows(ClassTask task, DayOfWeek weekday,
                                     LocalTime start, LocalTime end, List<String> reasons) {
        for (Student student : task.students()) {
            List<StudentConstraint> constraints = context.studentWindows().get(student.getId());
            if (constraints == null || constraints.isEmpty()) {
                continue;
            }

            boolean blocked = constraints.stream().anyMatch(c ->
                    c.getWeekday() == weekday.getValue()
                            && TYPE_UNAVAILABLE.equals(c.getType())
                            && Booking.overlaps(c.getStartTime(), c.getEndTime(), start, end));
            if (blocked) {
                reasons.add("学生" + student.getName() + "在该时段不可排");
                continue;
            }

            boolean hasWhitelist = constraints.stream().anyMatch(c -> TYPE_AVAILABLE.equals(c.getType()));
            if (hasWhitelist) {
                boolean allowed = constraints.stream().anyMatch(c ->
                        c.getWeekday() == weekday.getValue()
                                && TYPE_AVAILABLE.equals(c.getType())
                                && Booking.covers(c.getStartTime(), c.getEndTime(), start, end));
                if (!allowed) {
                    reasons.add("学生" + student.getName() + "的可用时段不包含该时段");
                }
            }
        }
    }

    /** 教师、教室、学生三方在同一时段都只能被占用一次 */
    private void checkOccupancy(ClassTask task, Long classroomId, Classroom classroom,
                                LocalDate date, LocalTime start, LocalTime end,
                                Set<Long> studentIds, List<String> reasons) {
        for (Booking booking : placed) {
            if (!booking.sameTimeAs(date, start, end)) {
                continue;
            }
            if (booking.teacherId().equals(task.teacher().getId())) {
                reasons.add("教师" + task.teacher().getName() + "在该时段已有课");
            }
            if (booking.classroomId().equals(classroomId)) {
                String name = classroom == null ? String.valueOf(classroomId) : classroom.getName();
                reasons.add("教室" + name + "在该时段已被占用");
            }
            if (!Collections.disjoint(booking.studentIds(), studentIds)) {
                reasons.add("该班有学生在该时段已有其他课");
            }
        }
    }
}
