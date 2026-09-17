package com.kelifang.attendance;

import com.kelifang.common.BizException;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

/**
 * 五种签到状态对应的课时扣减规则，写死在这里。
 * 出勤 1.0 / 迟到 1.0 / 早退 0.5 / 请假 0 / 旷课 1.0（机构规定：不来也扣）。
 */
public final class AttendanceRule {

    public static final Map<String, BigDecimal> CONSUMED_HOURS = Map.of(
            "PRESENT", new BigDecimal("1.0"),
            "LATE", new BigDecimal("1.0"),
            "EARLY_LEAVE", new BigDecimal("0.5"),
            "LEAVE", BigDecimal.ZERO,
            "ABSENT", new BigDecimal("1.0"));

    /**
     * 教师确实上了课的状态。教师教学课时按"这节课有没有人真来"算，
     * 和学员扣了多少课时无关 —— 一节课排出来就是一节教学课时，不随人数翻倍。
     */
    public static final Set<String> TAUGHT = Set.of("PRESENT", "LATE", "EARLY_LEAVE");

    private AttendanceRule() {
    }

    public static BigDecimal hoursOf(String status) {
        // 先判 null：Map.of 生成的不可变 map 对 null key 会抛 NPE，而不是返回 null
        BigDecimal hours = status == null ? null : CONSUMED_HOURS.get(status);
        if (hours == null) {
            throw BizException.badRequest("签到状态只能是 PRESENT / LATE / EARLY_LEAVE / LEAVE / ABSENT");
        }
        return hours;
    }
}
