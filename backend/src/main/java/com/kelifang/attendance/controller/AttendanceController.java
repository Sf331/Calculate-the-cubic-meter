package com.kelifang.attendance.controller;

import com.kelifang.attendance.dto.AttendanceSubmitRequest;
import com.kelifang.attendance.service.AttendanceService;
import com.kelifang.attendance.vo.ConsumeResult;
import com.kelifang.attendance.vo.SessionView;
import com.kelifang.common.Result;
import com.kelifang.schedule.vo.ScheduleView;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    /**
     * 可点名的课次列表。教师只看到自己的课，教务校长看到全部。
     * 路径写成 /lessons（不是 /{scheduleId}），避免和下面按 id 查的接口撞上。
     */
    @GetMapping("/lessons")
    public Result<List<ScheduleView>> lessons(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return Result.ok(attendanceService.lessons(from, to));
    }

    /** 点名页数据：这节课的信息 + 全班名单，名单上带已点状态和剩余课时。 */
    @GetMapping("/{scheduleId}")
    public Result<SessionView> session(@PathVariable Long scheduleId) {
        return Result.ok(attendanceService.session(scheduleId));
    }

    /**
     * 提交点名。这一个请求落库的是四样东西：
     * 签到记录 + 课时流水 + 教师工时 + 收入确认，同一事务。
     */
    @PostMapping("/{scheduleId}")
    public Result<ConsumeResult> consume(@PathVariable Long scheduleId,
                                         @RequestBody AttendanceSubmitRequest request) {
        return Result.ok(attendanceService.consume(scheduleId, request.getItems()));
    }
}
