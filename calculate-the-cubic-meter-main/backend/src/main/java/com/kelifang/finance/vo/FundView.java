package com.kelifang.finance.vo;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 收支明细的一行。带学生姓名，校长看报表时不该看到裸 id。 */
public record FundView(
        Long id,
        Long studentId,
        String studentName,
        /** PRE_RECEIVE / RECEIVE_CONFIRM / REFUND / EXPENSE */
        String type,
        BigDecimal amount,
        /** IN / OUT */
        String direction,
        /** 来源单据：预收指向课时账户，确认收入指向签到 */
        Long refId,
        LocalDate occurDate,
        String remark) {
}
