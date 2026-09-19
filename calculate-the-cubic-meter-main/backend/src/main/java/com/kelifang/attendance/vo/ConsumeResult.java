package com.kelifang.attendance.vo;

import java.math.BigDecimal;

/**
 * 一次点名提交的结果，直接把"三处联动"的数字摆出来：
 * 扣了多少课时、确认了多少收入、给老师记了多少工时。
 */
public record ConsumeResult(
        Long scheduleId,
        int attendanceCount,
        BigDecimal consumedHours,
        BigDecimal confirmedAmount,
        BigDecimal workhourAmount) {
}
