package com.kelifang.homework.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("homework_submission")
public class HomeworkSubmission {
    @TableId(type = IdType.AUTO) private Long id;
    private Long homeworkId;
    private Long studentId;
    private LocalDateTime submitTime;
    private String attachmentPaths;
    private BigDecimal score;
    private String status;
    private LocalDateTime createdAt;
}
