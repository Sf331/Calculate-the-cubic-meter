package com.kelifang.homework.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@TableName("submission_answer")
public class SubmissionAnswer {
    @TableId(type = IdType.AUTO) private Long id;
    private Long submissionId;
    private Long questionId;
    private String answer;
    private BigDecimal score;
    private Boolean correct;
    private String comment;
}
