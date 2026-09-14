package com.kelifang.basedata.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kelifang.basedata.entity.Teacher;
import com.kelifang.basedata.entity.TeacherAvailability;
import com.kelifang.basedata.mapper.TeacherAvailabilityMapper;
import com.kelifang.basedata.mapper.TeacherMapper;
import com.kelifang.common.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeacherService extends ServiceImpl<TeacherMapper, Teacher> {

    public static final Set<String> SALARY_TYPES = Set.of("HOURLY", "BASE_HOURLY", "MONTHLY");

    private final TeacherAvailabilityMapper teacherAvailabilityMapper;

    public void check(Teacher teacher) {
        if (!StringUtils.hasText(teacher.getName())) {
            throw BizException.badRequest("教师姓名不能为空");
        }
        if (!StringUtils.hasText(teacher.getSubject())) {
            throw BizException.badRequest("所授科目不能为空");
        }
        if (!SALARY_TYPES.contains(teacher.getSalaryType())) {
            throw BizException.badRequest("计薪方式只能是 HOURLY / BASE_HOURLY / MONTHLY");
        }
        if (teacher.getBaseSalary() == null) {
            teacher.setBaseSalary(BigDecimal.ZERO);
        }
        if (teacher.getBaseSalary().signum() < 0) {
            throw BizException.badRequest("底薪不能为负数");
        }
    }

    /**
     * 批量取教师可用时段。排课引擎用。
     * map 里没有某个教师，表示这位教师没配过可用时段，引擎按"不受限"处理。
     */
    public Map<Long, List<TeacherAvailability>> windowsByTeacher(List<Long> teacherIds) {
        if (teacherIds.isEmpty()) {
            return Map.of();
        }
        return teacherAvailabilityMapper
                .selectList(Wrappers.<TeacherAvailability>lambdaQuery()
                        .in(TeacherAvailability::getTeacherId, teacherIds))
                .stream()
                .collect(Collectors.groupingBy(TeacherAvailability::getTeacherId));
    }
}
