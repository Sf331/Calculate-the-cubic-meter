package com.kelifang.attendance.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kelifang.attendance.entity.LessonAccount;
import com.kelifang.attendance.entity.LessonTransaction;
import com.kelifang.attendance.mapper.LessonAccountMapper;
import com.kelifang.attendance.mapper.LessonTransactionMapper;
import com.kelifang.attendance.vo.LessonAccountView;
import com.kelifang.basedata.entity.Course;
import com.kelifang.basedata.entity.Student;
import com.kelifang.basedata.service.CourseService;
import com.kelifang.basedata.service.StudentService;
import com.kelifang.common.BizException;
import com.kelifang.common.UserContext;
import com.kelifang.finance.service.FundService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LessonAccountService extends ServiceImpl<LessonAccountMapper, LessonAccount> {

    private final LessonTransactionMapper lessonTransactionMapper;
    private final FundService fundService;
    private final StudentService studentService;
    private final CourseService courseService;

    /** 课时账户列表。按当前登录用户的可见范围过滤，家长看不见别人家孩子的余额。 */
    public List<LessonAccountView> listView(Long requestedStudentId) {
        List<Long> scope = studentService.visibleStudentIds(requestedStudentId);
        if (scope != null && scope.isEmpty()) {
            return List.of();
        }

        var query = Wrappers.<LessonAccount>lambdaQuery()
                .orderByAsc(LessonAccount::getStudentId)
                .orderByAsc(LessonAccount::getCourseId);
        if (scope != null) {
            query.in(LessonAccount::getStudentId, scope);
        }

        List<LessonAccount> accounts = list(query);
        if (accounts.isEmpty()) {
            return List.of();
        }

        Map<Long, Student> students = studentService.listByIds(
                        accounts.stream().map(LessonAccount::getStudentId).distinct().toList())
                .stream().collect(Collectors.toMap(Student::getId, Function.identity()));
        Map<Long, Course> courses = courseService.listByIds(
                        accounts.stream().map(LessonAccount::getCourseId).distinct().toList())
                .stream().collect(Collectors.toMap(Course::getId, Function.identity()));
        Map<Long, BigDecimal> unitPrices = unitPrices(accounts);

        return accounts.stream().map(account -> {
            Student student = students.get(account.getStudentId());
            Course course = courses.get(account.getCourseId());
            return new LessonAccountView(
                    account.getId(),
                    account.getStudentId(),
                    student == null ? null : student.getName(),
                    account.getCourseId(),
                    course == null ? null : course.getName(),
                    account.getTotalHours(),
                    account.getConsumedHours(),
                    account.getRemainingHours(),
                    unitPrices.get(account.getId()));
        }).toList();
    }

    /**
     * 取学员在某课程下的账户。没有账户说明还没收过费，直接拒绝核销。
     * 错误信息带上学生姓名 —— 一批点名失败时老师说得出是卡在谁身上。
     */
    public LessonAccount accountOf(Long studentId, Long courseId, String studentName) {
        LessonAccount account = getOne(Wrappers.<LessonAccount>lambdaQuery()
                .eq(LessonAccount::getStudentId, studentId)
                .eq(LessonAccount::getCourseId, courseId));
        if (account == null) {
            throw BizException.badRequest(studentName + " 还没有这门课的课时账户，请先收费开课");
        }
        return account;
    }

    public List<LessonAccount> accountsOfStudents(List<Long> studentIds) {
        if (studentIds.isEmpty()) {
            return List.of();
        }
        return list(Wrappers.<LessonAccount>lambdaQuery()
                .in(LessonAccount::getStudentId, studentIds)
                .orderByAsc(LessonAccount::getStudentId)
                .orderByAsc(LessonAccount::getCourseId));
    }

    public LessonAccount require(Long accountId) {
        LessonAccount account = getById(accountId);
        if (account == null) {
            throw BizException.notFound("课时账户不存在");
        }
        return account;
    }

    public List<LessonTransaction> transactions(Long accountId) {
        return lessonTransactionMapper.selectList(Wrappers.<LessonTransaction>lambdaQuery()
                .eq(LessonTransaction::getAccountId, accountId)
                .orderByDesc(LessonTransaction::getId));
    }

    /**
     * 某个时间段内全部课时流水。课时结转表用。
     *
     * 按 created_at 归月，因为 lesson_transaction 上只有这一个时间列。
     * demo 里点名发生在课次当天，和按课次日期归月是一回事。
     */
    public List<LessonTransaction> transactionsBetween(LocalDate from, LocalDate to) {
        return lessonTransactionMapper.selectList(Wrappers.<LessonTransaction>lambdaQuery()
                .ge(LessonTransaction::getCreatedAt, from.atStartOfDay())
                .le(LessonTransaction::getCreatedAt, to.plusDays(1).atStartOfDay())
                .orderByAsc(LessonTransaction::getCreatedAt)
                .orderByAsc(LessonTransaction::getId));
    }

    /**
     * 课时单价 = 该学员该课程已收费总额 / 已购课时数。
     * 没收费记录或没买课时时返回 0，不抛异常 —— 排课时就存在的账户不该因为没交钱而报错。
     */
    public BigDecimal unitPrice(LessonAccount account, BigDecimal paid) {
        BigDecimal bought = account.getTotalHours();
        if (bought == null || bought.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        return paid.divide(bought, 2, RoundingMode.HALF_UP);
    }

    /** 批量算单价，账户列表页用，避免一个账户查一次库。 */
    public Map<Long, BigDecimal> unitPrices(List<LessonAccount> accounts) {
        Map<Long, BigDecimal> paid = fundService.paidTotals(
                accounts.stream().map(LessonAccount::getId).toList());

        return accounts.stream().collect(Collectors.toMap(
                LessonAccount::getId,
                account -> unitPrice(account, paid.getOrDefault(account.getId(), BigDecimal.ZERO))));
    }

    /**
     * 收费开课。没有账户就开一个，有就往上加 —— 演示时"现场招个学生、收完费马上点名"靠这一步。
     *
     * 三处同一事务：账户余额、课时流水（RECHARGE）、资金流水（预收 IN）。
     * 收了钱没加课时或者加了课时没记账，都是演示现场会被问住的那种数据。
     */
    @Transactional
    public LessonAccountView recharge(Long studentId, Long courseId, BigDecimal hours,
                                      BigDecimal amount, String remark) {
        requireCanRecharge();
        if (hours == null || hours.signum() <= 0) {
            throw BizException.badRequest("充值课时必须大于 0");
        }
        if (amount == null || amount.signum() <= 0) {
            throw BizException.badRequest("收费金额必须大于 0");
        }

        Student student = studentService.getById(studentId);
        if (student == null) {
            throw BizException.notFound("学生不存在");
        }
        Course course = courseService.getById(courseId);
        if (course == null) {
            throw BizException.notFound("课程不存在");
        }

        String memo = StringUtils.hasText(remark) ? remark : "收费开课：" + course.getName();

        LessonAccount account = getOne(Wrappers.<LessonAccount>lambdaQuery()
                .eq(LessonAccount::getStudentId, studentId)
                .eq(LessonAccount::getCourseId, courseId));
        if (account == null) {
            account = new LessonAccount();
            account.setStudentId(studentId);
            account.setCourseId(courseId);
            account.setTotalHours(hours);
            account.setConsumedHours(BigDecimal.ZERO);
            account.setRemainingHours(hours);
            save(account);
        } else {
            account.setTotalHours(account.getTotalHours().add(hours));
            account.setRemainingHours(account.getRemainingHours().add(hours));
            updateById(account);
        }

        LessonTransaction tx = new LessonTransaction();
        tx.setStudentId(studentId);
        tx.setAccountId(account.getId());
        tx.setType("RECHARGE");
        tx.setHours(hours);
        tx.setBalanceAfter(account.getRemainingHours());
        tx.setRemark(memo);
        lessonTransactionMapper.insert(tx);

        fundService.receivePrePayment(studentId, amount, account.getId(), LocalDate.now(), memo);

        return new LessonAccountView(account.getId(), studentId, student.getName(),
                courseId, course.getName(), account.getTotalHours(), account.getConsumedHours(),
                account.getRemainingHours(),
                unitPrice(account, fundService.paidTotal(account.getId())));
    }

    /** 收费是钱的事，学生家长教师都碰不到，前端藏菜单不算数。 */
    private void requireCanRecharge() {
        String role = UserContext.role();
        if (!"PRINCIPAL".equals(role) && !"ACADEMIC".equals(role)) {
            throw BizException.forbidden("只有校长和教务能收费开课");
        }
    }

    /**
     * 扣课时。写流水和更新余额必须在同一事务里，且余额只由流水累加得出，不允许直接覆盖。
     *
     * hours 为 0（请假）时不产生流水：余额没动，记一条 0 的流水只是噪音。
     */
    public LessonTransaction consume(LessonAccount account, BigDecimal hours,
                                     Long attendanceId, String remark) {
        if (hours.signum() == 0) {
            return null;
        }
        if (account.getRemainingHours().compareTo(hours) < 0) {
            throw BizException.badRequest("课时不足：剩余 " + account.getRemainingHours()
                    + " 课时，本次需扣 " + hours + " 课时");
        }

        BigDecimal delta = hours.negate();
        BigDecimal balanceAfter = account.getRemainingHours().add(delta);

        LessonTransaction tx = new LessonTransaction();
        tx.setStudentId(account.getStudentId());
        tx.setAccountId(account.getId());
        tx.setType("CONSUME");
        tx.setHours(delta);
        tx.setBalanceAfter(balanceAfter);
        tx.setRefId(attendanceId);
        tx.setRemark(remark);
        lessonTransactionMapper.insert(tx);

        account.setConsumedHours(account.getConsumedHours().add(hours));
        account.setRemainingHours(balanceAfter);
        updateById(account);

        return tx;
    }
}
