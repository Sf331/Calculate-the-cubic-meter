package com.kelifang.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_user")
public class SysUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    /** demo 阶段明文，上线前必须换成哈希 */
    private String password;

    private String realName;

    /** PRINCIPAL / ACADEMIC / TEACHER / STUDENT / PARENT */
    private String role;

    private String phone;

    private LocalDateTime createdAt;
}
