package com.kelifang.salary.vo;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 工资单里的一条快照。存进 payslip.detail 的 JSON 里，之后不再重算。
 * attendanceId 是溯源用的：从这一条能点回产生这笔钱的签到记录。
 */
public record PayslipItem(
        Long scheduleId,
        LocalDate workDate,
        String className,
        String courseName,
        Integer studentCount,
        BigDecimal rate,
        BigDecimal amount,
        Long attendanceId) {
}
