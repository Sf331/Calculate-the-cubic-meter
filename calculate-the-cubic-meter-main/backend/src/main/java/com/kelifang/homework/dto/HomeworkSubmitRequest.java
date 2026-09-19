package com.kelifang.homework.dto;
import lombok.Data;
import java.util.List;
@Data
public class HomeworkSubmitRequest {
    private List<Answer> answers;
    private List<String> attachmentPaths;
    @Data public static class Answer {
        private Long questionId;
        private String answer;
    }
}
