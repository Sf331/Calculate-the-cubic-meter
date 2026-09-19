package com.kelifang.salary.controller;

import com.kelifang.common.Result;
import com.kelifang.salary.service.PayslipService;
import com.kelifang.salary.service.WorkhourService;
import com.kelifang.salary.vo.PayslipDetail;
import com.kelifang.salary.vo.PayslipView;
import com.kelifang.salary.vo.WorkhourView;
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
import java.util.Map;

@RestController
@RequestMapping("/api/salary")
@RequiredArgsConstructor
public class SalaryController {

    private final WorkhourService workhourService;
    private final PayslipService payslipService;

    /** 工时明细。教师只拿得到自己的（teacherId 由后端按登录身份改写）。 */
    @GetMapping("/workhour")
    public Result<List<WorkhourView>> workhours(
            @RequestParam(required = false) Long teacherId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return Result.ok(workhourService.listByTeacher(
                payslipService.scopeOf(teacherId), from, to));
    }

    /** 工资单列表。period 传 yyyy-MM。 */
    @GetMapping("/payslip")
    public Result<List<PayslipView>> payslips(
            @RequestParam(required = false) String period,
            @RequestParam(required = false) Long teacherId) {
        return Result.ok(payslipService.list(period, teacherId));
    }

    /** 生成某个月的工资单，已有则整张覆盖。 */
    @PostMapping("/payslip/generate")
    public Result<List<PayslipView>> generate(@RequestBody Map<String, String> body) {
        return Result.ok(payslipService.generate(body.get("period")));
    }

    /** 工资单详情（逐项快照）。教师查别人的单会被 403。 */
    @GetMapping("/payslip/{id}")
    public Result<PayslipDetail> detail(@PathVariable Long id) {
        return Result.ok(payslipService.detail(id));
    }
}
