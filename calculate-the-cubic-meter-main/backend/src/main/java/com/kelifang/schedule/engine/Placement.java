package com.kelifang.schedule.engine;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.Set;

/**
 * 一节可挪动的课。增量重排以它为单位做"搬走—重放"。
 * 比 Booking 多带 scheduleId 和 locked —— 前者用来定位数据库行，后者决定能不能挪。
 */
public record Placement(
        Long scheduleId,
        Long classId,
        Long teacherId,
        Long classroomId,
        LocalDate date,
        LocalTime start,
        LocalTime end,
        Set<Long> studentIds,
        boolean locked) {

    public Placement movedTo(LocalDate newDate, LocalTime newStart, LocalTime newEnd, Long newClassroomId) {
        return new Placement(scheduleId, classId, teacherId, newClassroomId,
                newDate, newStart, newEnd, studentIds, locked);
    }

    public Booking toBooking() {
        return new Booking(classId, teacherId, classroomId, date, start, end, studentIds);
    }

    /** 两节课是否抢同一个资源：同一位教师、同一间教室，或共用了任何一个学生。 */
    public boolean clashesWith(Placement other) {
        if (!date.equals(other.date())) {
            return false;
        }
        if (!Booking.overlaps(start, end, other.start, other.end)) {
            return false;
        }
        return teacherId.equals(other.teacherId())
                || classroomId.equals(other.classroomId())
                || !Collections.disjoint(studentIds, other.studentIds());
    }
}
