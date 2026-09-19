package com.kelifang.salary.vo;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 工时明细的一行：哪节课、哪个班、几个学生、算了多少钱。 */
public record WorkhourView(
        Long id,
        Long teacherId,
        String teacherName,
        Long scheduleId,
        String className,
        String courseName,
        LocalDate workDate,
        Integer studentCount,
        BigDecimal rate,
        BigDecimal amount,
        /** 点进签到记录的入口 */
        Long attendanceId) {
}
