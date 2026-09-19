package com.kelifang.homework.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("homework")
public class Homework {
    @TableId(type = IdType.AUTO) private Long id;
    private Long classId;
    private String name;
    private String questionIds;
    private BigDecimal totalScore;
    private LocalDateTime dueTime;
    private Long publisherId;
    private LocalDateTime createdAt;
}
