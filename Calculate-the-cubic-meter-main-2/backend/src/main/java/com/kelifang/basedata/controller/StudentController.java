package com.kelifang.basedata.controller;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kelifang.basedata.entity.Student;
import com.kelifang.basedata.service.StudentService;
import com.kelifang.common.BaseCrudController;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/basedata/student")
@RequiredArgsConstructor
public class StudentController extends BaseCrudController<Student> {

    private final StudentService studentService;

    @Override
    protected IService<Student> service() {
        return studentService;
    }

    @Override
    protected void check(Student entity) {
        studentService.check(entity);
    }
}
