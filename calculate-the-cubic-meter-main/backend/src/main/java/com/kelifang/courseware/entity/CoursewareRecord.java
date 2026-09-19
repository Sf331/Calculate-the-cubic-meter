package com.kelifang.courseware.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.kelifang.common.IdEntity;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("courseware_record")
public class CoursewareRecord implements IdEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long coursewareId;
    private Long scheduleId;
    private Long studentId;
    private String componentId;
    private String answer;
    private Boolean correct;
    private LocalDateTime createdAt;
}
