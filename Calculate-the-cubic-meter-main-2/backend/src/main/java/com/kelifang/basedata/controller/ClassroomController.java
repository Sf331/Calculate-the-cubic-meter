package com.kelifang.basedata.controller;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kelifang.basedata.entity.Classroom;
import com.kelifang.basedata.service.ClassroomService;
import com.kelifang.common.BaseCrudController;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/basedata/classroom")
@RequiredArgsConstructor
public class ClassroomController extends BaseCrudController<Classroom> {

    private final ClassroomService classroomService;

    @Override
    protected IService<Classroom> service() {
        return classroomService;
    }

    @Override
    protected void check(Classroom entity) {
        classroomService.check(entity);
    }
}
