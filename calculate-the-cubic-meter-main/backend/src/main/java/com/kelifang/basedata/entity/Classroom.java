package com.kelifang.basedata.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.kelifang.common.IdEntity;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("classroom")
public class Classroom implements IdEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属校区，为 null 表示不限定校区 */
    private Long campusId;

    private String name;

    private Integer capacity;

    @JsonIgnore
    @TableLogic
    private Integer deleted;

    private LocalDateTime createdAt;
}
