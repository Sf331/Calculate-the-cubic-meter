package com.kelifang.basedata.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kelifang.basedata.entity.Student;
import com.kelifang.basedata.entity.StudentConstraint;
import com.kelifang.basedata.mapper.StudentConstraintMapper;
import com.kelifang.basedata.mapper.StudentMapper;
import com.kelifang.common.BizException;
import com.kelifang.common.UserContext;
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

    /** 学生本人。学生端只能看到自己。 */
    public Student byUserId(Long userId) {
        if (userId == null) {
            return null;
        }
        return getOne(Wrappers.<Student>lambdaQuery().eq(Student::getUserId, userId));
    }

    /** 家长的孩子，可能不止一个。家长端看课时余额用。 */
    public List<Long> studentIdsOfParent(Long parentUserId) {
        if (parentUserId == null) {
            return List.of();
        }
        return list(Wrappers.<Student>lambdaQuery().eq(Student::getParentUserId, parentUserId))
                .stream()
                .map(Student::getId)
                .toList();
    }

    /**
     * 当前登录用户能看到哪些学生的数据。后端过滤，不靠前端藏菜单 ——
     * 演示时评委拿家长账号直接敲 URL 是能敲到别的孩子的余额的，那属于硬伤。
     *
     * 返回 null 表示不筛（校长/教务看全部）；返回不含任何合法 id 的列表表示"什么都看不到"，
     * 不用空列表是因为空列表配 in() 会把条件丢掉，反倒变成查全部。
     *
     * 教师没有这个入口：他看自己班学生的余额走签到页，那里已经按"是不是我的课"卡住了。
     */
    public List<Long> visibleStudentIds(Long requested) {
        String role = UserContext.role();
        if ("STUDENT".equals(role)) {
            Student self = byUserId(UserContext.userId());
            return self == null ? List.of(-1L) : List.of(self.getId());
        }
        if ("PARENT".equals(role)) {
            List<Long> children = studentIdsOfParent(UserContext.userId());
            if (children.isEmpty()) {
                return List.of(-1L);
            }
            return requested != null && children.contains(requested) ? List.of(requested) : children;
        }
        if ("TEACHER".equals(role)) {
            throw BizException.forbidden("教师不能查看课时账户列表");
        }
        return requested == null ? null : List.of(requested);
    }
}
