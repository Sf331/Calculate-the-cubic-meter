package com.kelifang.salary.vo;

import java.math.BigDecimal;
import java.util.List;

/** 工资单详情：逐项快照。点进某一条能回到产生这笔钱的签到。 */
public record PayslipDetail(
        Long id,
        Long teacherId,
        String teacherName,
        String period,
        BigDecimal totalAmount,
        List<PayslipItem> items) {
}
