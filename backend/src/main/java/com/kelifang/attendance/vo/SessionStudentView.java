package com.kelifang.attendance.vo;

import java.math.BigDecimal;

/** 点名页的一行：学生 + 已点状态 + 这门课还剩多少课时。 */
public record SessionStudentView(
        Long studentId,
        String studentName,
        /** 已点过名的状态，没点过为 null */
        String status,
        BigDecimal consumedHours,
        BigDecimal remainingHours,
        BigDecimal unitPrice) {
}
