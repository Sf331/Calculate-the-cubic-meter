package com.kelifang.finance.vo;

import java.math.BigDecimal;

/** 班级毛利表的一行：这个班确认了多少收入，对应多少教师成本。 */
public record ClassProfitRow(
        Long classId,
        String className,
        String courseName,
        BigDecimal revenue,
        BigDecimal cost,
        BigDecimal profit) {
}
