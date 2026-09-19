package com.kelifang.finance.controller;

import com.kelifang.basedata.service.StudentService;
import com.kelifang.common.Result;
import com.kelifang.finance.service.FundService;
import com.kelifang.finance.vo.FundView;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/finance")
@RequiredArgsConstructor
public class FundController {

    private final FundService fundService;
    private final StudentService studentService;

    /**
     * 资金流水，也就是"收支明细"这张报表。
     * 签到核销产生的"冲预收 + 确认收入"两条在这里看得到，
     * ref_id 指回 attendance，点得进去就知道这笔钱是哪次点名产生的。
     */
    @GetMapping("/transaction")
    public Result<List<FundView>> list(
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String type) {
        return Result.ok(fundService.views(
                studentService.visibleStudentIds(studentId), from, to, type));
    }
}
