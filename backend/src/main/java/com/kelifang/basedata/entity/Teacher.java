package com.kelifang.basedata.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.kelifang.common.IdEntity;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("teacher")
public class Teacher implements IdEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联 sys_user，为 null 表示这位教师还没有登录账号 */
    private Long userId;

    private String name;

    private String subject;

    private Long campusId;

    /** HOURLY 纯课时费 / BASE_HOURLY 底薪+课时费 / MONTHLY 纯月薪 */
    private String salaryType;

    private BigDecimal baseSalary;

    @JsonIgnore
    @TableLogic
    private Integer deleted;

    private LocalDateTime createdAt;
}
