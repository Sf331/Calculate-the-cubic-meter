package com.kelifang.basedata.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalTime;

/** 教师可用时段。没有配任何行的教师视为不受限。 */
@Data
@TableName("teacher_availability")
public class TeacherAvailability {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long teacherId;

    /** 1=周一 … 7=周日，对应 java.time.DayOfWeek */
    private Integer weekday;

    private LocalTime startTime;

    private LocalTime endTime;
}
