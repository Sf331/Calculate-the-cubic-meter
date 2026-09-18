package com.kelifang.homework.dto;
import lombok.Data;
import java.math.BigDecimal;
@Data
public class QuestionRequest {
    private String subject;
    private String grade;
    private String knowledgePoint;
    private String type;
    private String stem;
    private String options;
    private String answer;
    private BigDecimal score;
}
