package com.kelifang.basedata.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalTime;

/** 学生可排 / 不可排时段。没有配任何行的学生视为不受限。 */
@Data
@TableName("student_constraint")
public class StudentConstraint {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentId;

    /** AVAILABLE 可排时段 / UNAVAILABLE 绝对不可排时段 */
    private String type;

    private Integer weekday;

    private LocalTime startTime;

    private LocalTime endTime;
}
