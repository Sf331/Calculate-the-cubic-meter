package com.kelifang.finance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kelifang.common.IdEntity;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 资金流水。只增不改，报表全部从这一张表聚合出来，不另建台账表。
 *
 * ref_id 的两种含义：
 * - PRE_RECEIVE 时指向 lesson_account.id，用来算"该学员该课程已收费总额"
 * - RECEIVE_CONFIRM 时指向 attendance.id，报表能一路溯源回具体哪次签到
 */
@Data
@TableName("fund_transaction")
public class FundTransaction implements IdEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentId;

    /** PRE_RECEIVE / RECEIVE_CONFIRM / REFUND / EXPENSE */
    private String type;

    private BigDecimal amount;

    /** IN / OUT */
    private String direction;

    private Long refId;

    private LocalDate occurDate;

    private String remark;

    private LocalDateTime createdAt;
}
