package com.kelifang.courseware.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kelifang.common.IdEntity;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("courseware")
public class Courseware implements IdEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    /** Stored as text deliberately: this works with both H2 and MySQL. */
    private String schemaJson;
    private LocalDateTime createdAt;
}
