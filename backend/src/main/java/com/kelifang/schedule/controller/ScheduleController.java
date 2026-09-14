package com.kelifang.schedule.controller;

import com.kelifang.common.Result;
import com.kelifang.schedule.dto.GenerateRequest;
import com.kelifang.schedule.dto.MoveRequest;
import com.kelifang.schedule.dto.RescheduleRequest;
import com.kelifang.schedule.service.ScheduleService;
import com.kelifang.schedule.vo.GenerateResult;
import com.kelifang.schedule.vo.ReschedulePlan;
import com.kelifang.schedule.vo.ScheduleView;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/schedule")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    @PostMapping("/generate")
    public Result<GenerateResult> generate(@RequestBody GenerateRequest request) {
        return Result.ok(scheduleService.generate(request.getStartDate(), request.getWeeks()));
    }

    /**
     * 课表查询。四类课表就是同一个接口的四种筛选：
     * 班级课表按 classId、教师课表按 teacherId、教室课表按 classroomId、学生课表按 studentId。
     */
    @GetMapping
    public Result<List<ScheduleView>> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Long teacherId,
            @RequestParam(required = false) Long classroomId,
            @RequestParam(required = false) Long studentId) {
        return Result.ok(scheduleService.list(from, to, classId, teacherId, classroomId, studentId));
    }

    /** 清空未锁定的课表。 */
    @DeleteMapping
    public Result<Void> clear() {
        scheduleService.clear();
        return Result.ok();
    }

    /**
     * 手动把一节课挪到新时段。
     * 返回被违反的硬约束：空数组表示挪成功，非空表示被拒且每条就是原因。
     */
    @PutMapping("/{id}/move")
    public Result<List<String>> move(@PathVariable Long id, @RequestBody MoveRequest request) {
        return Result.ok(scheduleService.move(
                id, request.getLessonDate(), request.getStartTime(), request.getClassroomId()));
    }

    /**
     * 增量重排：把一节课挪到新时段，撞到的课由引擎自动另找位置。
     * dryRun = true 只出方案不落库，前端先展示影响面，教务确认后再传 false。
     */
    @PostMapping("/{id}/reschedule")
    public Result<ReschedulePlan> reschedule(@PathVariable Long id,
                                             @RequestBody RescheduleRequest request) {
        return Result.ok(scheduleService.reschedule(
                id, request.getLessonDate(), request.getStartTime(),
                request.getClassroomId(), Boolean.TRUE.equals(request.getDryRun())));
    }
}
