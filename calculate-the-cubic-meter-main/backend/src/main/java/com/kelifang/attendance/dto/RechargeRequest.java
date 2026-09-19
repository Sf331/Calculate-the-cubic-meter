package com.kelifang.attendance.dto;

import lombok.Data;

import java.math.BigDecimal;

/** 收费开课：给某个学员的某门课加课时，同时收一笔预收款。 */
@Data
public class RechargeRequest {

    private Long studentId;

    private Long courseId;

    private BigDecimal hours;

    private BigDecimal amount;

    private String remark;
}
