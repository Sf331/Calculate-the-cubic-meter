package com.kelifang.attendance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kelifang.common.IdEntity;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 课时流水。只增不改，账户余额由本表累加得出。 */
@Data
@TableName("lesson_transaction")
public class LessonTransaction implements IdEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentId;

    private Long accountId;

    /** RECHARGE / CONSUME / REFUND / GIFT / ADJUST */
    private String type;

    /** 带正负号 */
    private BigDecimal hours;

    /** 本次变动后的余额 */
    private BigDecimal balanceAfter;

    /** 来源单据 id，核销时指向 attendance.id */
    private Long refId;

    private String remark;

    private LocalDateTime createdAt;
}
