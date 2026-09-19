package com.kelifang.schedule.engine;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 教学时段网格，也就是计划书里说的"合规时段白名单"。
 * 排课不能超出这个范围，这是硬约束之一。
 *
 * 写死在常量里：demo 阶段不做机构自定义营业时间，那要加表和页面，收益不抵成本。
 */
public final class TeachingGrid {

    /** 工作日最早开课时间 */
    public static final LocalTime WEEKDAY_START = LocalTime.of(16, 0);

    /** 周末最早开课时间 */
    public static final LocalTime WEEKEND_START = LocalTime.of(9, 0);

    /** 每天最晚结束时间 */
    public static final LocalTime LATEST_END = LocalTime.of(21, 0);

    /** 候选开课时间的粒度 */
    public static final int STEP_MINUTES = 30;

    private TeachingGrid() {
    }

    public static LocalTime dayStart(DayOfWeek weekday) {
        return switch (weekday) {
            case SATURDAY, SUNDAY -> WEEKEND_START;
            default -> WEEKDAY_START;
        };
    }

    /** 某天所有可能的开课时间点。按课时长截断，保证不会超过最晚结束时间。 */
    public static List<LocalTime> startTimes(DayOfWeek weekday, int durationMinutes) {
        List<LocalTime> result = new ArrayList<>();
        LocalTime last = LATEST_END.minusMinutes(durationMinutes);
        for (LocalTime t = dayStart(weekday); !t.isAfter(last); t = t.plusMinutes(STEP_MINUTES)) {
            result.add(t);
        }
        return result;
    }

    /** 时段是否完整落在当天的教学网格内 */
    public static boolean withinGrid(DayOfWeek weekday, LocalTime start, LocalTime end) {
        return !start.isBefore(dayStart(weekday)) && !end.isAfter(LATEST_END);
    }
}
