package com.kelifang.schedule.engine;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

/**
 * 一节已经排定的课。既是本次求解已经放下的结果，也是从数据库读出来的"锁定课表"。
 * 校验新课时拿它做占用比对。
 */
public record Booking(
        Long classId,
        Long teacherId,
        Long classroomId,
        LocalDate date,
        LocalTime start,
        LocalTime end,
        Set<Long> studentIds) {

    public boolean sameTimeAs(LocalDate otherDate, LocalTime otherStart, LocalTime otherEnd) {
        return date.equals(otherDate) && overlaps(start, end, otherStart, otherEnd);
    }

    /** 半开区间 [aStart, aEnd) 与 [bStart, bEnd) 是否相交 */
    public static boolean overlaps(LocalTime aStart, LocalTime aEnd, LocalTime bStart, LocalTime bEnd) {
        return aStart.isBefore(bEnd) && bStart.isBefore(aEnd);
    }

    /** [start, end) 是否完整落在 [windowStart, windowEnd) 内 */
    public static boolean covers(LocalTime windowStart, LocalTime windowEnd,
                                 LocalTime start, LocalTime end) {
        return !start.isBefore(windowStart) && !end.isAfter(windowEnd);
    }
}
