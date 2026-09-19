package com.kelifang.content.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.kelifang.common.IdEntity;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 内容资产。讲义、试卷、课件、音视频都在这张表里。
 * 版本是内联的 —— 只存最新一份文件，`versionNo` 改一次加一，没有历史。
 */
@Data
@TableName("content_asset")
public class ContentAsset implements IdEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String subject;

    private String grade;

    /** 按课表推送时按本字段匹配 */
    private Long courseId;

    private String knowledgePoint;

    /** COURSEWARE / HANDOUT / PAPER / MEDIA */
    private String type;

    /** StorageUtil 落盘后的相对文件名，通过 /uploads/** 访问。没传文件时为空 */
    private String filePath;

    /** 改一次加一。不存历史，所以这个数字只是"改过几回" */
    private Integer versionNo;

    /** ALL = 所有人可见 / PRIVATE = 只有 owner 和校长教务可见 */
    private String scope;

    /** 上传者的 sys_user.id，不是 teacherId */
    private Long ownerId;

    @JsonIgnore
    @TableLogic
    private Integer deleted;

    private LocalDateTime createdAt;
}
