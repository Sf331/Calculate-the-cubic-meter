package com.kelifang.basedata.controller;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kelifang.basedata.entity.Teacher;
import com.kelifang.basedata.service.TeacherService;
import com.kelifang.common.BaseCrudController;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/basedata/teacher")
@RequiredArgsConstructor
public class TeacherController extends BaseCrudController<Teacher> {

    private final TeacherService teacherService;

    @Override
    protected IService<Teacher> service() {
        return teacherService;
    }

    @Override
    protected void check(Teacher entity) {
        teacherService.check(entity);
    }
}
