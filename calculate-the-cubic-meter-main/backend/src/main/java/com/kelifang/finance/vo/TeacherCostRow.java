package com.kelifang.finance.vo;

import java.math.BigDecimal;

/** 教师成本表的一行。 */
public record TeacherCostRow(
        Long teacherId,
        String teacherName,
        /** 这个月上了几节课 */
        int lessonCount,
        /** 这些课累计有多少人次出勤 */
        int studentCount,
        BigDecimal amount) {
}
