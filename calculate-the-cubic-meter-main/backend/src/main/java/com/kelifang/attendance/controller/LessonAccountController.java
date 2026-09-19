package com.kelifang.attendance.controller;

import com.kelifang.attendance.dto.RechargeRequest;
import com.kelifang.attendance.entity.LessonTransaction;
import com.kelifang.attendance.service.LessonAccountService;
import com.kelifang.attendance.vo.LessonAccountView;
import com.kelifang.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/lesson-account")
@RequiredArgsConstructor
public class LessonAccountController {

    private final LessonAccountService lessonAccountService;

    /** 课时账户。学生看自己、家长看孩子、校长教务看全部，范围由后端定，不认前端传什么。 */
    @GetMapping
    public Result<List<LessonAccountView>> list(
            @RequestParam(required = false) Long studentId) {
        return Result.ok(lessonAccountService.listView(studentId));
    }

    /** 某个账户的全部流水，只增不改，倒序。 */
    @GetMapping("/{id}/transaction")
    public Result<List<LessonTransaction>> transactions(@PathVariable Long id) {
        return Result.ok(lessonAccountService.transactions(id));
    }

    /** 收费开课。没有账户顺手开一个，返回充值后的账户状态。 */
    @PostMapping("/recharge")
    public Result<LessonAccountView> recharge(@RequestBody RechargeRequest request) {
        return Result.ok(lessonAccountService.recharge(
                request.getStudentId(),
                request.getCourseId(),
                request.getHours(),
                request.getAmount(),
                request.getRemark()));
    }
}
