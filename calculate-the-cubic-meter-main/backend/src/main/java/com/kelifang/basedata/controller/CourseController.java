package com.kelifang.basedata.controller;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kelifang.basedata.entity.Course;
import com.kelifang.basedata.service.CourseService;
import com.kelifang.common.BaseCrudController;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/basedata/course")
@RequiredArgsConstructor
public class CourseController extends BaseCrudController<Course> {

    private final CourseService courseService;

    @Override
    protected IService<Course> service() {
        return courseService;
    }

    @Override
    protected void check(Course entity) {
        courseService.check(entity);
    }
}
