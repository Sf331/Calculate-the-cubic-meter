package com.kelifang.schedule.vo;

import java.util.List;

/**
 * 一次调课的完整方案与影响面。
 * feasible = false 时 moves 为空，reasons 说明为什么挪不动。
 */
public record ReschedulePlan(
        boolean feasible,
        List<RescheduleMove> moves,
        /** 受影响的班级名，去重 */
        List<String> affectedClasses,
        /** 受影响的教师名，去重 */
        List<String> affectedTeachers,
        /** 受影响的学生人数（去重） */
        int affectedStudents,
        List<String> reasons) {
}
