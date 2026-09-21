package com.kelifang.notice.service;

import com.kelifang.common.BizException;
import com.kelifang.common.CurrentUser;
import com.kelifang.common.UserContext;
import com.kelifang.notice.entity.Notice;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 站内通知。**不落库**，数据就存在这个进程内的列表里，重启即清空。
 *
 * 发布方是校长 / 教务 / 教师（和前端菜单的 roles 对齐），收方是学生 / 家长。
 * 可见范围由后端按登录角色算，不认前端传什么 —— 学生直接请求接口也拿不到别人的通知。
 */
@Service
public class NoticeService {

    /** 能发通知的角色 */
    private static final Set<String> PUBLISHER_ROLES = Set.of("PRINCIPAL", "ACADEMIC", "TEACHER");
    /** 收通知的角色 */
    private static final Set<String> RECEIVER_ROLES = Set.of("STUDENT", "PARENT");

    public static final String AUDIENCE_ALL = "ALL";
    private static final Set<String> AUDIENCES = Set.of(AUDIENCE_ALL, "STUDENT", "PARENT");

    /** 标题 / 内容长度上限，跟 schema.sql 里 notification 表的字段宽度取齐 */
    private static final int TITLE_MAX = 200;
    private static final int CONTENT_MAX = 500;

    private final List<Notice> notices = new CopyOnWriteArrayList<>();
    private final AtomicLong sequence = new AtomicLong();

    /** 通知列表，新的在前。发布方看到全部，学生 / 家长只看到发给自己的。 */
    public List<Notice> list() {
        String role = requireLogin();

        List<Notice> visible = new ArrayList<>();
        for (Notice notice : notices) {
            if (PUBLISHER_ROLES.contains(role) || canReceive(role, notice.getAudience())) {
                visible.add(notice);
            }
        }
        Collections.reverse(visible);
        return visible;
    }

    /** 发布一条通知。发布者和发布时间由后端填，请求里的同名字段一律忽略。 */
    public Notice publish(Notice request) {
        CurrentUser publisher = requirePublisher();

        String title = trim(request.getTitle());
        if (title.isEmpty()) {
            throw BizException.badRequest("通知标题不能为空");
        }
        if (title.length() > TITLE_MAX) {
            throw BizException.badRequest("通知标题不能超过 " + TITLE_MAX + " 个字");
        }

        String content = trim(request.getContent());
        if (content.length() > CONTENT_MAX) {
            throw BizException.badRequest("通知内容不能超过 " + CONTENT_MAX + " 个字");
        }

        String audience = trim(request.getAudience());
        if (audience.isEmpty()) {
            audience = AUDIENCE_ALL;
        }
        if (!AUDIENCES.contains(audience)) {
            throw BizException.badRequest("面向对象只能是全体、仅学生或仅家长");
        }

        Notice notice = new Notice();
        notice.setId(sequence.incrementAndGet());
        notice.setTitle(title);
        notice.setContent(content);
        notice.setAudience(audience);
        notice.setPublisherId(publisher.getId());
        notice.setPublisherName(publisher.getRealName());
        notice.setPublisherRole(publisher.getRole());
        notice.setCreatedAt(LocalDateTime.now());
        notices.add(notice);
        return notice;
    }

    /** 撤回已发布的通知。只允许发布方角色操作。 */
    public void delete(Long id) {
        requirePublisher();
        if (!notices.removeIf(notice -> Objects.equals(notice.getId(), id))) {
            throw BizException.notFound("通知不存在或已被删除");
        }
    }

    /** 收方可见性：面向全体，或面向自己这个角色（STUDENT / PARENT）。 */
    private boolean canReceive(String role, String audience) {
        return RECEIVER_ROLES.contains(role)
                && (AUDIENCE_ALL.equals(audience) || role.equals(audience));
    }

    private CurrentUser requirePublisher() {
        CurrentUser user = requireCurrentUser();
        if (!PUBLISHER_ROLES.contains(user.getRole())) {
            throw BizException.forbidden("只有校长、教务和教师可以发布通知");
        }
        return user;
    }

    private CurrentUser requireCurrentUser() {
        CurrentUser user = UserContext.get();
        if (user == null) {
            throw BizException.forbidden("未登录");
        }
        return user;
    }

    private String requireLogin() {
        return requireCurrentUser().getRole();
    }

    /** null 安全并去首尾空白 */
    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
