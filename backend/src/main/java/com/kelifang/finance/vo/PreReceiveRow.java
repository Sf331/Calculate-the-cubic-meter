package com.kelifang.finance.vo;

import java.math.BigDecimal;

/** 预收台账的一行：收了多少钱、确认了多少收入、还剩多少预收挂着。 */
public record PreReceiveRow(
        Long studentId,
        String studentName,
        /** 累计收取的预收款 */
        BigDecimal received,
        /** 已按课时消耗确认的收入 */
        BigDecimal confirmed,
        /** 还挂在预收账款上的余额 */
        BigDecimal balance) {
}
