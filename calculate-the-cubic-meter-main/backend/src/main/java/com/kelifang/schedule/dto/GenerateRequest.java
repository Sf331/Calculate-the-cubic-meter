package com.kelifang.schedule.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class GenerateRequest {

    /** 起始日期，作为第 1 周的周一（不强制校验是不是周一） */
    private LocalDate startDate;

    /** 排几周 */
    private Integer weeks;
}
