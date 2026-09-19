package com.kelifang.finance.vo;

import java.math.BigDecimal;
import java.util.List;

/**
 * 经营看板。校长登录第一眼看的数字。
 * 口径全部按"课时消耗发生的那天"（也就是课次日期）归属，不按操作时间。
 */
public record DashboardView(
        /** yyyy-MM */
        String period,
        BigDecimal confirmedRevenue,
        BigDecimal consumedHours,
        BigDecimal preReceiveBalance,
        BigDecimal teacherCost,
        int lessonCount,
        /** 这个月有签到的课次里，出勤（含迟到早退）的人次 */
        int presentCount,
        int studentCount,
        int teacherCount,
        int classCount,
        /** 本月逐日趋势 */
        List<DailyPoint> daily) {

    /** 趋势图的一个点。按天给收入和课时消耗，看板画两条线。 */
    public record DailyPoint(String date, BigDecimal revenue, BigDecimal hours) {
    }
}
