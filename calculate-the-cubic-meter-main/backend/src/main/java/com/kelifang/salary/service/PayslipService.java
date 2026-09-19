package com.kelifang.salary.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kelifang.basedata.entity.Teacher;
import com.kelifang.basedata.service.TeacherService;
import com.kelifang.common.BizException;
import com.kelifang.common.UserContext;
import com.kelifang.salary.entity.Payslip;
import com.kelifang.salary.mapper.PayslipMapper;
import com.kelifang.salary.vo.PayslipDetail;
import com.kelifang.salary.vo.PayslipItem;
import com.kelifang.salary.vo.PayslipView;
import com.kelifang.salary.vo.WorkhourView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PayslipService extends ServiceImpl<PayslipMapper, Payslip> {

    private final WorkhourService workhourService;
    private final TeacherService teacherService;
    private final ObjectMapper objectMapper;

    /**
     * 生成某个月的工资单：按教师分组，把逐条工时做成快照冻进 detail。
     *
     * 已有该月工资单就整张覆盖 —— demo 不做"反审核"那一套。
     * 口径是固定课时单价 × 有效教学课时，每节课一条工时，所以 Σ工时金额 就是工资。
     *
     * period 传空则按本月算。
     */
    @Transactional
    public List<PayslipView> generate(String period) {
        YearMonth month = parsePeriod(period);
        List<WorkhourView> rows = workhourService.listByTeacher(
                null, month.atDay(1), month.atEndOfMonth());

        Map<Long, List<WorkhourView>> byTeacher = rows.stream()
                .collect(Collectors.groupingBy(WorkhourView::teacherId,
                        LinkedHashMap::new, Collectors.toList()));

        List<Payslip> slips = new ArrayList<>();
        for (Map.Entry<Long, List<WorkhourView>> entry : byTeacher.entrySet()) {
            Payslip slip = getOne(Wrappers.<Payslip>lambdaQuery()
                    .eq(Payslip::getTeacherId, entry.getKey())
                    .eq(Payslip::getPeriod, month.toString()));
            if (slip == null) {
                slip = new Payslip();
                slip.setTeacherId(entry.getKey());
                slip.setPeriod(month.toString());
            }

            List<PayslipItem> items = entry.getValue().stream()
                    .map(row -> new PayslipItem(row.scheduleId(), row.workDate(),
                            row.className(), row.courseName(), row.studentCount(),
                            row.rate(), row.amount(), row.attendanceId()))
                    .toList();

            slip.setTotalAmount(items.stream()
                    .map(PayslipItem::amount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            slip.setDetail(toJson(items));
            saveOrUpdate(slip);
            slips.add(slip);
        }
        return toViews(slips);
    }

    /** 工资单列表。教师只拿得到自己的，传别人的 teacherId 也没用。 */
    public List<PayslipView> list(String period, Long teacherId) {
        var query = Wrappers.<Payslip>lambdaQuery()
                .orderByDesc(Payslip::getPeriod)
                .orderByAsc(Payslip::getTeacherId);
        if (StringUtils.hasText(period)) {
            query.eq(Payslip::getPeriod, parsePeriod(period).toString());
        }
        Long scoped = scopeOf(teacherId);
        if (scoped != null) {
            query.eq(Payslip::getTeacherId, scoped);
        }
        return toViews(list(query));
    }

    /** 工资单详情。教师查别人的单会被拒，不是在列表里藏起来就算完。 */
    public PayslipDetail detail(Long id) {
        Payslip slip = getById(id);
        if (slip == null) {
            throw BizException.notFound("工资单不存在");
        }
        Long scoped = scopeOf(null);
        if (scoped != null && !scoped.equals(slip.getTeacherId())) {
            throw BizException.forbidden("只能查看本人的工资单");
        }
        return toDetail(slip);
    }

    /**
     * 当前登录用户能看的教师范围。教师返回自己的 teacherId，其余角色返回传进来的值
     * （null 表示不筛）。后端过滤，不靠前端藏菜单。工时明细和工资单都走它。
     */
    public Long scopeOf(Long requested) {
        if (!"TEACHER".equals(UserContext.role())) {
            return requested;
        }
        Teacher teacher = teacherService.byUserId(UserContext.userId());
        if (teacher == null) {
            throw BizException.forbidden("当前账号没有关联教师，看不到工资单");
        }
        return teacher.getId();
    }

    private YearMonth parsePeriod(String period) {
        if (!StringUtils.hasText(period)) {
            return YearMonth.now();
        }
        try {
            return YearMonth.parse(period);
        } catch (DateTimeParseException e) {
            throw BizException.badRequest("月份格式应为 yyyy-MM，例如 2026-09");
        }
    }

    private List<PayslipView> toViews(List<Payslip> slips) {
        if (slips.isEmpty()) {
            return List.of();
        }
        Map<Long, Teacher> teachers = teacherService.listByIds(
                        slips.stream().map(Payslip::getTeacherId).distinct().toList())
                .stream().collect(Collectors.toMap(Teacher::getId, Function.identity()));

        return slips.stream().map(slip -> {
            Teacher teacher = teachers.get(slip.getTeacherId());
            return new PayslipView(
                    slip.getId(),
                    slip.getTeacherId(),
                    teacher == null ? null : teacher.getName(),
                    slip.getPeriod(),
                    slip.getTotalAmount(),
                    itemsOf(slip).size());
        }).toList();
    }

    private PayslipDetail toDetail(Payslip slip) {
        Teacher teacher = teacherService.getById(slip.getTeacherId());
        return new PayslipDetail(
                slip.getId(),
                slip.getTeacherId(),
                teacher == null ? null : teacher.getName(),
                slip.getPeriod(),
                slip.getTotalAmount(),
                itemsOf(slip));
    }

    /** 快照存的是裸 JSON 文本（原因见 Payslip.detail 的注释），显式序列化。 */
    private String toJson(List<PayslipItem> items) {
        try {
            return objectMapper.writeValueAsString(items);
        } catch (JsonProcessingException e) {
            throw new BizException("工资单快照写入失败：" + e.getMessage());
        }
    }

    private List<PayslipItem> itemsOf(Payslip slip) {
        if (!StringUtils.hasText(slip.getDetail())) {
            return List.of();
        }
        try {
            return objectMapper.readValue(slip.getDetail(), new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new BizException("工资单快照解析失败：" + e.getMessage());
        }
    }
}
