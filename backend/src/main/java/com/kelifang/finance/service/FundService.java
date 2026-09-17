package com.kelifang.finance.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kelifang.finance.entity.FundTransaction;
import com.kelifang.finance.mapper.FundTransactionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 资金流水。只增不改 —— 预收、收入确认、退费、支出全是这张表里的行。
 *
 * 模块之间不直接查对方的 Mapper，所以课时单价要用的"已收费总额"从这里取，
 * 而不是由 attendance 模块自己去 select fund_transaction。
 */
@Service
@RequiredArgsConstructor
public class FundService extends ServiceImpl<FundTransactionMapper, FundTransaction> {

    /** 批量取账户的已收费总额（key 为 lesson_account.id）。报表和账户页都要用。 */
    public Map<Long, BigDecimal> paidTotals(List<Long> accountIds) {
        if (accountIds.isEmpty()) {
            return Map.of();
        }
        return list(Wrappers.<FundTransaction>lambdaQuery()
                .eq(FundTransaction::getType, "PRE_RECEIVE")
                .eq(FundTransaction::getDirection, "IN")
                .in(FundTransaction::getRefId, accountIds))
                .stream()
                .collect(Collectors.groupingBy(FundTransaction::getRefId,
                        Collectors.reducing(BigDecimal.ZERO, FundTransaction::getAmount, BigDecimal::add)));
    }

    public BigDecimal paidTotal(Long accountId) {
        return paidTotals(List.of(accountId)).getOrDefault(accountId, BigDecimal.ZERO);
    }

    /**
     * 按课时消耗确认收入。写在同一事务里，金额一个子儿不差：
     * 冲减预收账款（OUT）+ 确认当期收入（IN），
     * ref_id 都指向 attendance.id，报表能一路溯源回签到。
     */
    public BigDecimal confirmRevenue(Long studentId, BigDecimal unitPrice, BigDecimal hours,
                                     Long attendanceId, LocalDate occurDate) {
        BigDecimal amount = unitPrice.multiply(hours).setScale(2, RoundingMode.HALF_UP);
        if (amount.signum() == 0) {
            return BigDecimal.ZERO;
        }

        FundTransaction preReceive = new FundTransaction();
        preReceive.setStudentId(studentId);
        preReceive.setType("PRE_RECEIVE");
        preReceive.setAmount(amount);
        preReceive.setDirection("OUT");
        preReceive.setRefId(attendanceId);
        preReceive.setOccurDate(occurDate);
        preReceive.setRemark("课时消耗冲减预收");
        save(preReceive);

        FundTransaction confirm = new FundTransaction();
        confirm.setStudentId(studentId);
        confirm.setType("RECEIVE_CONFIRM");
        confirm.setAmount(amount);
        confirm.setDirection("IN");
        confirm.setRefId(attendanceId);
        confirm.setOccurDate(occurDate);
        confirm.setRemark("课时消耗确认收入");
        save(confirm);

        return amount;
    }

    /** 按学生列流水。studentIds 传 null 表示不筛（校长/教务看全部）。 */
    public List<FundTransaction> listByStudents(List<Long> studentIds) {
        var query = Wrappers.<FundTransaction>lambdaQuery()
                .orderByDesc(FundTransaction::getOccurDate)
                .orderByDesc(FundTransaction::getId);

        if (studentIds != null) {
            if (studentIds.isEmpty()) {
                return List.of();
            }
            query.in(FundTransaction::getStudentId, studentIds);
        }
        return list(query);
    }
}
