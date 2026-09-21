package com.kelifang.notice.controller;

import com.kelifang.common.Result;
import com.kelifang.notice.entity.Notice;
import com.kelifang.notice.service.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 站内通知。列表按登录角色返回不同范围，见 {@link NoticeService}。
 */
@RestController
@RequestMapping("/api/notice")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    /** 通知列表。校长 / 教务 / 教师拿到全部（发布页），学生 / 家长只拿发给自己的。 */
    @GetMapping("/list")
    public Result<List<Notice>> list() {
        return Result.ok(noticeService.list());
    }

    /** 发布通知。 */
    @PostMapping
    public Result<Notice> publish(@RequestBody Notice request) {
        return Result.ok(noticeService.publish(request));
    }

    /** 撤回通知。 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        noticeService.delete(id);
        return Result.ok();
    }
}
