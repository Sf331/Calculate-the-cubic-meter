package com.kelifang.finance.vo;

import java.math.BigDecimal;

/** 课时结转表的一行：某个账户某个月的期初 + 充值 − 消耗 = 期末。 */
public record LessonCarryRow(
        Long studentId,
        String studentName,
        Long courseId,
        String courseName,
        /** yyyy-MM */
        String period,
        BigDecimal opening,
        BigDecimal recharged,
        BigDecimal consumed,
        BigDecimal closing) {
}
