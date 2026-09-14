package com.kelifang.basedata.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kelifang.basedata.entity.Student;
import com.kelifang.basedata.mapper.StudentMapper;
import com.kelifang.common.BizException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class StudentService extends ServiceImpl<StudentMapper, Student> {

    public void check(Student student) {
        if (!StringUtils.hasText(student.getName())) {
            throw BizException.badRequest("学生姓名不能为空");
        }
        if (!StringUtils.hasText(student.getGrade())) {
            throw BizException.badRequest("年级不能为空");
        }
    }
}
