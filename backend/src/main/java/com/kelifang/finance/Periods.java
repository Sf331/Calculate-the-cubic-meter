package com.kelifang.finance;

import com.kelifang.common.BizException;
import org.springframework.util.StringUtils;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;

/** 报表的月份口径。period 一律是 yyyy-MM，不传就按当前月。 */
public final class Periods {

    private Periods() {
    }

    public static YearMonth parse(String period) {
        if (!StringUtils.hasText(period)) {
            return YearMonth.now();
        }
        try {
            return YearMonth.parse(period);
        } catch (DateTimeParseException e) {
            throw BizException.badRequest("月份格式应为 yyyy-MM，例如 2026-09");
        }
    }
}
