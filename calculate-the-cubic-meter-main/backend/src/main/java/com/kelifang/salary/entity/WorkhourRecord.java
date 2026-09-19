package com.kelifang.salary.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kelifang.common.IdEntity;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 工时明细，签到核销时自动生成 —— 没有有效签到就不可能有工时记录。
 * 一节课一行，不按学生人数累加。
 */
@Data
@TableName("workhour_record")
public class WorkhourRecord implements IdEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long teacherId;

    private Long scheduleId;

    /** 追溯到产生本工时的签到记录（这节课第一条有效签到） */
    private Long attendanceId;

    private LocalDate workDate;

    /** 实际出勤人数 */
    private Integer studentCount;

    /** 本次应用的课时单价 */
    private BigDecimal rate;

    private BigDecimal amount;

    private LocalDateTime createdAt;
}
