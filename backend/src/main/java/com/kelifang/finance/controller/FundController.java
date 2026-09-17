package com.kelifang.finance.controller;

import com.kelifang.basedata.service.StudentService;
import com.kelifang.common.Result;
import com.kelifang.finance.entity.FundTransaction;
import com.kelifang.finance.service.FundService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/finance")
@RequiredArgsConstructor
public class FundController {

    private final FundService fundService;
    private final StudentService studentService;

    /**
     * 资金流水。签到核销产生的"冲预收 + 确认收入"两条能在这里看到，
     * ref_id 指回 attendance，点得进去就知道这笔钱是哪次点名产生的。
     */
    @GetMapping("/transaction")
    public Result<List<FundTransaction>> list(@RequestParam(required = false) Long studentId) {
        return Result.ok(fundService.listByStudents(studentService.visibleStudentIds(studentId)));
    }
}
