package com.kelifang.schedule.vo;

import java.time.LocalDate;
import java.time.LocalTime;

/** 方案里的一次挪动。from 是原位置，to 是新位置。 */
public record RescheduleMove(
        Long scheduleId,
        String className,
        String courseName,
        String teacherName,
        LocalDate fromDate,
        LocalTime fromStart,
        LocalTime fromEnd,
        String fromClassroomName,
        LocalDate toDate,
        LocalTime toStart,
        LocalTime toEnd,
        String toClassroomName) {
}
