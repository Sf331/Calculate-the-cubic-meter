package com.kelifang.schedule.engine;

import com.kelifang.basedata.entity.Clazz;
import com.kelifang.basedata.entity.Course;
import com.kelifang.basedata.entity.Student;
import com.kelifang.basedata.entity.Teacher;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** 排课的基本单位：一个班级连同它需要的课程、教师和学生名单。 */
public record ClassTask(Clazz clazz, Course course, Teacher teacher, List<Student> students) {

    public Set<Long> studentIds() {
        return students.stream().map(Student::getId).collect(Collectors.toSet());
    }
}
