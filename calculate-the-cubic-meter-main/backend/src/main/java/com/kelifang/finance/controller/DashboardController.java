package com.kelifang.finance.controller;

import com.kelifang.common.BizException;
import com.kelifang.common.Result;
import com.kelifang.common.UserContext;
import com.kelifang.finance.Periods;
import com.kelifang.finance.service.DashboardService;
import com.kelifang.finance.vo.DashboardView;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 经营看板。只有校长能看。 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public Result<DashboardView> get(@RequestParam(required = false) String period) {
        if (!"PRINCIPAL".equals(UserContext.role())) {
            throw BizException.forbidden("只有校长能看经营看板");
        }
        return Result.ok(dashboardService.of(Periods.parse(period)));
    }
}
