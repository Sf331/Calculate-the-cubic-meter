package com.kelifang.basedata.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kelifang.basedata.entity.Clazz;
import com.kelifang.basedata.entity.ClassStudent;
import com.kelifang.basedata.entity.Student;
import com.kelifang.basedata.mapper.ClazzMapper;
import com.kelifang.basedata.mapper.ClassStudentMapper;
import com.kelifang.basedata.mapper.StudentMapper;
import com.kelifang.common.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClazzService extends ServiceImpl<ClazzMapper, Clazz> {

    private final ClassStudentMapper classStudentMapper;
    private final StudentMapper studentMapper;

    public void check(Clazz clazz) {
        if (!StringUtils.hasText(clazz.getName())) {
            throw BizException.badRequest("班级名称不能为空");
        }
        // 没有课程或没有授课教师的班级，排课引擎无法处理
        if (clazz.getCourseId() == null) {
            throw BizException.badRequest("必须选择所属课程");
        }
        if (clazz.getTeacherId() == null) {
            throw BizException.badRequest("必须指定授课教师");
        }
        if (clazz.getCapacity() == null) {
            clazz.setCapacity(0);
        }
        if (clazz.getCapacity() < 0) {
            throw BizException.badRequest("班级容量不能为负数");
        }
    }

    /** 班级学生名单。排课引擎靠它判断学生时段冲突。 */
    public List<Student> students(Long classId) {
        return studentsByClass(List.of(classId)).getOrDefault(classId, List.of());
    }

    /**
     * 批量取多个班级的名单，一次查完，避免排课时按班级循环查库。
     * 已逻辑删除的学生会在关联表里留下孤儿行，这里靠 selectBatchIds 天然过滤掉。
     */
    public Map<Long, List<Student>> studentsByClass(List<Long> classIds) {
        if (classIds.isEmpty()) {
            return Map.of();
        }

        List<ClassStudent> rows = classStudentMapper.selectList(
                Wrappers.<ClassStudent>lambdaQuery().in(ClassStudent::getClassId, classIds));
        if (rows.isEmpty()) {
            return Map.of();
        }

        Map<Long, Student> byId = studentMapper
                .selectBatchIds(rows.stream().map(ClassStudent::getStudentId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(Student::getId, s -> s));

        return rows.stream()
                .filter(row -> byId.containsKey(row.getStudentId()))
                .collect(Collectors.groupingBy(ClassStudent::getClassId,
                        Collectors.mapping(row -> byId.get(row.getStudentId()), Collectors.toList())));
    }

    /** 某位学生在哪些班级里。学生个人课表靠它筛。 */
    public List<Long> classIdsOfStudent(Long studentId) {
        return classStudentMapper
                .selectList(Wrappers.<ClassStudent>lambdaQuery()
                        .eq(ClassStudent::getStudentId, studentId))
                .stream()
                .map(ClassStudent::getClassId)
                .toList();
    }

    /** 整体替换名单。前端是"提交一整个名单"，不是逐个增删，所以直接删了重建。 */
    @Transactional
    public void setStudents(Long classId, List<Long> studentIds) {
        Clazz clazz = getById(classId);
        if (clazz == null) {
            throw BizException.notFound("班级不存在");
        }

        List<Long> ids = studentIds == null ? List.of() : studentIds.stream().distinct().toList();

        // 容量为 0 表示不限，不校验
        Integer capacity = clazz.getCapacity();
        if (capacity != null && capacity > 0 && ids.size() > capacity) {
            throw BizException.badRequest(
                    "班级容量为 " + capacity + " 人，不能放入 " + ids.size() + " 名学生");
        }

        classStudentMapper.delete(
                Wrappers.<ClassStudent>lambdaQuery().eq(ClassStudent::getClassId, classId));

        for (Long studentId : ids) {
            ClassStudent row = new ClassStudent();
            row.setClassId(classId);
            row.setStudentId(studentId);
            classStudentMapper.insert(row);
        }
    }
}
