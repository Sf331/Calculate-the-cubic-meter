package com.kelifang.basedata.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.kelifang.common.IdEntity;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/** autoResultMap = true 是 tags 的 TypeHandler 能生效的前提 */
@Data
@TableName(value = "course", autoResultMap = true)
public class Course implements IdEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String subject;

    private String grade;

    private Integer durationMinutes;

    private Integer weeklyTimes;

    /** 特殊课型标记，存 JSON 文本列：TRIAL 试听 / MAKEUP 补课 / SUBSTITUTE 代课 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> tags;

    @JsonIgnore
    @TableLogic
    private Integer deleted;

    private LocalDateTime createdAt;
}
