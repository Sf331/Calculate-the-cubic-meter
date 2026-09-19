package com.kelifang.schedule.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kelifang.common.IdEntity;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** 一节课。四类课表（班级/教师/教室/学生）都是这张表的不同筛选视角。 */
@Data
@TableName("schedule")
public class Schedule implements IdEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long classId;

    private Long teacherId;

    private Long classroomId;

    private Long campusId;

    private LocalDate lessonDate;

    private LocalTime startTime;

    private LocalTime endTime;

    /** PLANNED / CANCELLED / DONE */
    private String status;

    /** 1 表示这行在增量重排时原样保留，不参与重新求解 */
    private Integer locked;

    private LocalDateTime createdAt;
}
