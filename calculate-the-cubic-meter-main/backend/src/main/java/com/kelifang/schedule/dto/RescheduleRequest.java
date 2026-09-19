package com.kelifang.schedule.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class RescheduleRequest {

    private LocalDate lessonDate;

    private LocalTime startTime;

    /** 不传则保持原教室 */
    private Long classroomId;

    /** true 只看方案不落库。前端"预览影响面"传 true，确认后传 false。 */
    private Boolean dryRun;
}
