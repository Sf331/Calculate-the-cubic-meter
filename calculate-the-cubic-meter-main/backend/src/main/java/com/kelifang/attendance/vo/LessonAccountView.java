package com.kelifang.attendance.vo;

import java.math.BigDecimal;

/** 课时账户页的一行。 */
public record LessonAccountView(
        Long id,
        Long studentId,
        String studentName,
        Long courseId,
        String courseName,
        BigDecimal totalHours,
        BigDecimal consumedHours,
        BigDecimal remainingHours,
        /** 课时单价 = 已收费总额 / 已购课时数 */
        BigDecimal unitPrice) {
}
