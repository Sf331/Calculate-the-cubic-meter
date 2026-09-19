package com.kelifang.salary.vo;

import java.math.BigDecimal;

/** 工资单列表的一行。 */
public record PayslipView(
        Long id,
        Long teacherId,
        String teacherName,
        /** yyyy-MM */
        String period,
        BigDecimal totalAmount,
        /** 这个月上了几节课 */
        int lessonCount) {
}
