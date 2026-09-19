package com.kelifang.schedule.vo;

import java.time.LocalDate;
import java.time.LocalTime;

/** 课表展示用。四类课表（班级/教师/教室/学生）都是这个结构的不同筛选。 */
public record ScheduleView(
        Long id,
        Long classId,
        String className,
        Long teacherId,
        String teacherName,
        Long classroomId,
        String classroomName,
        Long courseId,
        String courseName,
        String subject,
        LocalDate lessonDate,
        LocalTime startTime,
        LocalTime endTime,
        Integer locked,
        String status) {
}
