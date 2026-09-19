package com.kelifang.finance.service;

import com.kelifang.attendance.AttendanceRule;
import com.kelifang.attendance.entity.Attendance;
import com.kelifang.attendance.service.AttendanceService;
import com.kelifang.basedata.service.ClazzService;
import com.kelifang.basedata.service.StudentService;
import com.kelifang.basedata.service.TeacherService;
import com.kelifang.finance.entity.FundTransaction;
import com.kelifang.finance.vo.DashboardView;
import com.kelifang.salary.entity.WorkhourRecord;
import com.kelifang.salary.service.WorkhourService;
import com.kelifang.schedule.entity.Schedule;
import com.kelifang.schedule.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 经营看板。校长登录第一眼看的数字。
 *
 * 口径统一按**课次日期**归属（收入、课时消耗都算在课上的那天），
 * 所以点几次名之后回来看，数字是当场变的。
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final FundService fundService;
    private final AttendanceService attendanceService;
    private final WorkhourService workhourService;
    private final ScheduleService scheduleService;
    private final StudentService studentService;
    private final TeacherService teacherService;
    private final ClazzService clazzService;

    public DashboardView of(YearMonth month) {
        LocalDate from = month.atDay(1);
        LocalDate to = month.atEndOfMonth();

        List<FundTransaction> funds = fundService.query(null, from, to, null);
        BigDecimal revenue = funds.stream()
                .filter(row -> "RECEIVE_CONFIRM".equals(row.getType()))
                .map(FundTransaction::getAmount)
                .reduce(ZERO, BigDecimal::add);

        List<Attendance> attendances = attendanceService.between(from, to);
        BigDecimal consumedHours = attendances.stream()
                .map(Attendance::getConsumedHours)
                .filter(Objects::nonNull)
                .reduce(ZERO, BigDecimal::add);
        int presentCount = (int) attendances.stream()
                .filter(row -> AttendanceRule.TAUGHT.contains(row.getStatus()))
                .count();

        Map<Long, Schedule> schedules = attendances.isEmpty()
                ? Map.of()
                : scheduleService.listByIds(attendances.stream()
                        .map(Attendance::getScheduleId).distinct().toList()).stream()
                .collect(Collectors.toMap(Schedule::getId, Function.identity()));

        // 预收余额是累计数，算到本期末为止
        BigDecimal preReceiveBalance = fundService.query(null, null, to, "PRE_RECEIVE").stream()
                .map(row -> "IN".equals(row.getDirection())
                        ? row.getAmount()
                        : row.getAmount().negate())
                .reduce(ZERO, BigDecimal::add);

        BigDecimal teacherCost = workhourService.between(from, to).stream()
                .map(WorkhourRecord::getAmount)
                .reduce(ZERO, BigDecimal::add);

        return new DashboardView(
                month.toString(),
                revenue,
                consumedHours,
                preReceiveBalance,
                teacherCost,
                schedules.size(),
                presentCount,
                (int) studentService.count(),
                (int) teacherService.count(),
                (int) clazzService.count(),
                dailyPoints(month, funds, attendances, schedules));
    }

    /** 本月逐日趋势。没有数据的日子也要补 0，否则折线图 x 轴会跳。 */
    private List<DashboardView.DailyPoint> dailyPoints(YearMonth month,
                                                       List<FundTransaction> funds,
                                                       List<Attendance> attendances,
                                                       Map<Long, Schedule> schedules) {
        Map<LocalDate, BigDecimal[]> daily = new TreeMap<>();
        for (int day = 1; day <= month.lengthOfMonth(); day++) {
            daily.put(month.atDay(day), new BigDecimal[]{ZERO, ZERO});
        }

        for (FundTransaction row : funds) {
            if (!"RECEIVE_CONFIRM".equals(row.getType())) {
                continue;
            }
            BigDecimal[] slot = daily.get(row.getOccurDate());
            if (slot != null) {
                slot[0] = slot[0].add(row.getAmount());
            }
        }

        for (Attendance row : attendances) {
            Schedule schedule = schedules.get(row.getScheduleId());
            if (schedule == null || row.getConsumedHours() == null) {
                continue;
            }
            BigDecimal[] slot = daily.get(schedule.getLessonDate());
            if (slot != null) {
                slot[1] = slot[1].add(row.getConsumedHours());
            }
        }

        return daily.entrySet().stream()
                .map(entry -> new DashboardView.DailyPoint(
                        entry.getKey().toString(), entry.getValue()[0], entry.getValue()[1]))
                .toList();
    }
}
