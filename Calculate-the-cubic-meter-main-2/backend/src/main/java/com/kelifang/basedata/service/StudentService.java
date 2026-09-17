package com.kelifang.basedata.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kelifang.basedata.entity.Student;
import com.kelifang.basedata.entity.StudentConstraint;
import com.kelifang.basedata.mapper.StudentConstraintMapper;
import com.kelifang.basedata.mapper.StudentMapper;
import com.kelifang.common.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentService extends ServiceImpl<StudentMapper, Student> {

    private final StudentConstraintMapper studentConstraintMapper;

    public void check(Student student) {
        if (!StringUtils.hasText(student.getName())) {
            throw BizException.badRequest("学生姓名不能为空");
        }
        if (!StringUtils.hasText(student.getGrade())) {
            throw BizException.badRequest("年级不能为空");
        }
    }

    /**
     * 批量取学生排课约束。排课引擎用。
     * map 里没有某个学生，表示这位学生没配过约束，引擎按"不受限"处理。
     */
    public Map<Long, List<StudentConstraint>> constraintsByStudent(List<Long> studentIds) {
        if (studentIds.isEmpty()) {
            return Map.of();
        }
        return studentConstraintMapper
                .selectList(Wrappers.<StudentConstraint>lambdaQuery()
                        .in(StudentConstraint::getStudentId, studentIds))
                .stream()
                .collect(Collectors.groupingBy(StudentConstraint::getStudentId));
    }
}
