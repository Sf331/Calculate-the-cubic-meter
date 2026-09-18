package com.kelifang.homework.dto;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
@Data
public class GradeRequest {
    private List<Item> answers;
    @Data public static class Item {
        private Long questionId;
        private BigDecimal score;
        private String comment;
    }
}
