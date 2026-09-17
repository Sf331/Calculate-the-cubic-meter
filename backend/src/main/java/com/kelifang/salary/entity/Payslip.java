package com.kelifang.salary.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kelifang.common.IdEntity;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 工资单。一个月一位教师一张。
 *
 * detail 是**生成那一刻的逐项快照** —— 之后改课时单价、改签到记录，历史工资单不跟着变。
 * 这是财务报表该有的性质：已经结算过的月份不能被后来的改动改写。
 *
 * 这里刻意**不用** MyBatis-Plus 的 JacksonTypeHandler：它读 JSON 列时按原始类型
 * List.class 反序列化，元素会变成 LinkedHashMap，泛型信息全丢，
 * 再往外序列化就报 "object is not an instance of declaring class"。
 * 所以存成裸 JSON 文本，由 PayslipService 用 Spring 的 ObjectMapper 显式读写。
 */
@Data
@TableName("payslip")
public class Payslip implements IdEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long teacherId;

    /** yyyy-MM */
    private String period;

    private BigDecimal totalAmount;

    /** payslip.detail 列的原始 JSON 文本，解析见 PayslipService#items */
    private String detail;

    private LocalDateTime createdAt;
}
