package com.kelifang.homework.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("question")
public class Question {
    @TableId(type = IdType.AUTO) private Long id;
    private String subject;
    private String grade;
    private String knowledgePoint;
    private String type;
    private String stem;
    private String options;
    private String answer;
    private BigDecimal score;
    @TableLogic private Integer deleted;
    private LocalDateTime createdAt;
}
