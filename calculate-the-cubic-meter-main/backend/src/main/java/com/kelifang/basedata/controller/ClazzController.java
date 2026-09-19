package com.kelifang.basedata.controller;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kelifang.basedata.entity.Clazz;
import com.kelifang.basedata.entity.Student;
import com.kelifang.basedata.service.ClazzService;
import com.kelifang.common.BaseCrudController;
import com.kelifang.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/basedata/clazz")
@RequiredArgsConstructor
public class ClazzController extends BaseCrudController<Clazz> {

    private final ClazzService clazzService;

    @Override
    protected IService<Clazz> service() {
        return clazzService;
    }

    @Override
    protected void check(Clazz entity) {
        clazzService.check(entity);
    }

    /** 班级学生名单。不在基类里，因为只有班级有这个需求。 */
    @GetMapping("/{id}/student")
    public Result<List<Student>> students(@PathVariable Long id) {
        return Result.ok(clazzService.students(id));
    }

    /** 整体替换名单，前端提交的就是完整的一份。 */
    @PutMapping("/{id}/student")
    public Result<Void> setStudents(@PathVariable Long id, @RequestBody List<Long> studentIds) {
        clazzService.setStudents(id, studentIds);
        return Result.ok();
    }
}
