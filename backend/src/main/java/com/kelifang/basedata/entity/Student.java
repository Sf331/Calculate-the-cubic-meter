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
@TableName("student")
public class Student implements IdEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 学生自己的登录账号，为 null 表示还没开通 */
    private Long userId;

    /** 家长账号，家长端靠这个字段找到自己孩子的数据 */
    private Long parentUserId;

    private String name;

    private String grade;

    private Long campusId;

    @JsonIgnore
    @TableLogic
    private Integer deleted;

    private LocalDateTime createdAt;
}
