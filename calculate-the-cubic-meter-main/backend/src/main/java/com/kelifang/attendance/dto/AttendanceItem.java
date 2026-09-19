package com.kelifang.attendance.dto;

import lombok.Data;

/** 单个学生的点名结果。 */
@Data
public class AttendanceItem {

    private Long studentId;

    /** PRESENT / LATE / EARLY_LEAVE / LEAVE / ABSENT */
    private String status;
}
