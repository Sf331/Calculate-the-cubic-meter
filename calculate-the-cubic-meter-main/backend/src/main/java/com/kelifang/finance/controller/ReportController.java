package com.kelifang.finance.controller;

import com.kelifang.common.BizException;
import com.kelifang.common.Result;
import com.kelifang.common.UserContext;
import com.kelifang.finance.Periods;
import com.kelifang.finance.service.ReportService;
import com.kelifang.finance.vo.ClassProfitRow;
import com.kelifang.finance.vo.LessonCarryRow;
import com.kelifang.finance.vo.PreReceiveRow;
import com.kelifang.finance.vo.TeacherCostRow;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 财会报表。收支明细在 FundController（就是那份流水本身），这里另外四张。
 * 只有校长能看 —— 菜单也这么配，但边界在后端。
 */
@RestController
@RequestMapping("/api/finance/report")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/pre-receive")
    public Result<List<PreReceiveRow>> preReceive(@RequestParam(required = false) String period) {
        requirePrincipal();
        return Result.ok(reportService.preReceive(Periods.parse(period)));
    }

    @GetMapping("/lesson-carry")
    public Result<List<LessonCarryRow>> lessonCarry(@RequestParam(required = false) String period) {
        requirePrincipal();
        return Result.ok(reportService.lessonCarry(Periods.parse(period)));
    }

    @GetMapping("/teacher-cost")
    public Result<List<TeacherCostRow>> teacherCost(@RequestParam(required = false) String period) {
        requirePrincipal();
        return Result.ok(reportService.teacherCost(Periods.parse(period)));
    }

    @GetMapping("/class-profit")
    public Result<List<ClassProfitRow>> classProfit(@RequestParam(required = false) String period) {
        requirePrincipal();
        return Result.ok(reportService.classProfit(Periods.parse(period)));
    }

    private void requirePrincipal() {
        if (!"PRINCIPAL".equals(UserContext.role())) {
            throw BizException.forbidden("只有校长能看财会报表");
        }
    }
}
