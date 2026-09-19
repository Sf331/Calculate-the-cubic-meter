package com.kelifang.attendance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kelifang.common.IdEntity;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 学员在某个课程下的课时账户。一个学员同一门课只有一个账户。 */
@Data
@TableName("lesson_account")
public class LessonAccount implements IdEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentId;

    private Long courseId;

    /** 累计购买课时 */
    private BigDecimal totalHours;

    /** 累计消耗课时 */
    private BigDecimal consumedHours;

    /** 冗余字段，必须恒等于 lesson_transaction.hours 之和 */
    private BigDecimal remainingHours;

    private LocalDateTime createdAt;
}
