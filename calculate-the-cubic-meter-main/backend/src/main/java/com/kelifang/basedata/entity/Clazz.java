package com.kelifang.basedata.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.kelifang.common.IdEntity;
import lombok.Data;

import java.time.LocalDateTime;

/** 班级。类名不能叫 Class，所以是 Clazz，表名 clazz。 */
@Data
@TableName("clazz")
public class Clazz implements IdEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private Long courseId;

    private Long teacherId;

    /** 可以留空，排课时再指定教室 */
    private Long classroomId;

    private Long campusId;

    private Integer capacity;

    @JsonIgnore
    @TableLogic
    private Integer deleted;

    private LocalDateTime createdAt;
}
