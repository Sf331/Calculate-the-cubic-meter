package com.kelifang.notice.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 站内通知。
 *
 * **刻意不落库**：demo 只要"发布方发出去、学生和家长能看见"，
 * 数据活在 {@code NoticeService} 的进程内列表里，重启即清空 —— 和 H2 内存库一个待遇。
 * 所以这里没有 @TableName，也没有对应的 Mapper。
 */
@Data
public class Notice {

    private Long id;

    private String title;

    private String content;

    /** 面向对象：ALL=全体（学生 + 家长） / STUDENT=仅学生 / PARENT=仅家长 */
    private String audience;

    /** 发布者。后端从登录态填，前端只传 title / content / audience。 */
    private Long publisherId;
    private String publisherName;
    private String publisherRole;

    private LocalDateTime createdAt;
}
