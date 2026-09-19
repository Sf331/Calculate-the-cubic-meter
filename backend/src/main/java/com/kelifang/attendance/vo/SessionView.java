package com.kelifang.attendance.vo;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** 点名页：这节课的信息 + 全班名单。 */
public record SessionView(
        Long scheduleId,
        Long classId,
        String className,
        Long courseId,
        String courseName,
        Long teacherId,
        String teacherName,
        LocalDate lessonDate,
        LocalTime startTime,
        LocalTime endTime,
        /** PLANNED / DONE / CANCELLED */
        String status,
        List<SessionStudentView> students) {
}
