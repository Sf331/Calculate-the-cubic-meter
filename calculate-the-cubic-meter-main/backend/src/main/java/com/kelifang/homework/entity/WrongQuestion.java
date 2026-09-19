package com.kelifang.homework.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("wrong_question")
public class WrongQuestion {
    @TableId(type = IdType.AUTO) private Long id;
    private Long studentId;
    private Long questionId;
    private String knowledgePoint;
    private Integer wrongCount;
    private LocalDateTime lastWrongTime;
}
