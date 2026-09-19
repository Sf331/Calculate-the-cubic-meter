package com.kelifang.courseware.dto;
import lombok.Data;
@Data
public class CoursewareAnswerRequest {
    private Long scheduleId;
    private String componentId;
    private String answer;
}
