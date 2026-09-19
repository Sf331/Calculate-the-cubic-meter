package com.kelifang.homework.dto;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
@Data
public class HomeworkCreateRequest {
    private Long classId;
    private String name;
    private List<Long> questionIds;
    private LocalDateTime dueTime;
}
