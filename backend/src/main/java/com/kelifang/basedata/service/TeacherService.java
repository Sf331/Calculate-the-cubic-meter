package com.kelifang.basedata.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kelifang.basedata.entity.Teacher;
import com.kelifang.basedata.mapper.TeacherMapper;
import com.kelifang.common.BizException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Set;

@Service
public class TeacherService extends ServiceImpl<TeacherMapper, Teacher> {

    public static final Set<String> SALARY_TYPES = Set.of("HOURLY", "BASE_HOURLY", "MONTHLY");

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
}
