package com.kelifang.attendance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kelifang.common.IdEntity;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 签到记录。全链路凭证的起点：课时流水、工时、收入确认都指向这一行。 */
@Data
@TableName("attendance")
public class Attendance implements IdEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long scheduleId;

    private Long studentId;

    /** PRESENT / LATE / EARLY_LEAVE / LEAVE / ABSENT */
    private String status;

    /** TEACHER=教师点名 / CODE=学生口令 */
    private String signMethod;

    private LocalDateTime signTime;

    private BigDecimal consumedHours;

    private LocalDateTime createdAt;
}
