package com.kelifang.schedule.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class MoveRequest {

    private LocalDate lessonDate;

    /** 新的开始时间，结束时间按课程时长自动算 */
    private LocalTime startTime;

    /** 不传则保持原教室 */
    private Long classroomId;
}
