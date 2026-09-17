package com.kelifang.finance.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kelifang.basedata.entity.Student;
import com.kelifang.basedata.service.StudentService;
import com.kelifang.finance.entity.FundTransaction;
import com.kelifang.finance.mapper.FundTransactionMapper;
import com.kelifang.finance.vo.FundView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

    private final StudentService studentService;

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
     * 收取预收款：学员买课时，钱先进预收账款，等上课了再由 confirmRevenue 确认成收入。
     * ref_id 指向 lesson_account.id，课时单价才拿得到"这个账户一共收过多少钱"。
     */
    public void receivePrePayment(Long studentId, BigDecimal amount, Long accountId,
                                  LocalDate occurDate, String remark) {
        FundTransaction row = new FundTransaction();
        row.setStudentId(studentId);
        row.setType("PRE_RECEIVE");
        row.setAmount(amount);
        row.setDirection("IN");
        row.setRefId(accountId);
        row.setOccurDate(occurDate);
        row.setRemark(remark);
        save(row);
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

    /**
     * 查流水。studentIds 传 null 表示不筛（校长/教务看全部），
     * from/to/type 都可以不传。收支明细和报表都走这一个方法。
     */
    public List<FundTransaction> query(List<Long> studentIds, LocalDate from, LocalDate to, String type) {
        var query = Wrappers.<FundTransaction>lambdaQuery()
                .orderByDesc(FundTransaction::getOccurDate)
                .orderByDesc(FundTransaction::getId);

        if (studentIds != null) {
            if (studentIds.isEmpty()) {
                return List.of();
            }
            query.in(FundTransaction::getStudentId, studentIds);
        }
        if (from != null) {
            query.ge(FundTransaction::getOccurDate, from);
        }
        if (to != null) {
            query.le(FundTransaction::getOccurDate, to);
        }
        if (type != null) {
            query.eq(FundTransaction::getType, type);
        }
        return list(query);
    }

    /** 收支明细。多带一个学生姓名，报表页不该显示裸 id。 */
    public List<FundView> views(List<Long> studentIds, LocalDate from, LocalDate to, String type) {
        List<FundTransaction> rows = query(studentIds, from, to, type);
        if (rows.isEmpty()) {
            return List.of();
        }

        Map<Long, String> names = byNames(rows.stream()
                .map(FundTransaction::getStudentId)
                .filter(Objects::nonNull)
                .distinct()
                .toList());

        return rows.stream().map(row -> new FundView(
                row.getId(),
                row.getStudentId(),
                names.get(row.getStudentId()),
                row.getType(),
                row.getAmount(),
                row.getDirection(),
                row.getRefId(),
                row.getOccurDate(),
                row.getRemark())).toList();
    }

    private Map<Long, String> byNames(List<Long> studentIds) {
        if (studentIds.isEmpty()) {
            return Map.of();
        }
        return studentService.listByIds(studentIds).stream()
                .collect(Collectors.toMap(Student::getId, Student::getName));
    }
}
