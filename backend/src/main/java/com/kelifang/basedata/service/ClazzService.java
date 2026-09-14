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
        List<Long> studentIds = classStudentMapper
                .selectList(Wrappers.<ClassStudent>lambdaQuery().eq(ClassStudent::getClassId, classId))
                .stream()
                .map(ClassStudent::getStudentId)
                .toList();

        return studentIds.isEmpty() ? List.of() : studentMapper.selectBatchIds(studentIds);
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
