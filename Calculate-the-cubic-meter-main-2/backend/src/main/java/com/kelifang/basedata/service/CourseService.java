package com.kelifang.basedata.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kelifang.basedata.entity.Course;
import com.kelifang.basedata.mapper.CourseMapper;
import com.kelifang.common.BizException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;

@Service
public class CourseService extends ServiceImpl<CourseMapper, Course> {

    public static final Set<String> TAGS = Set.of("TRIAL", "MAKEUP", "SUBSTITUTE", "CROSS_CAMPUS");

    public void check(Course course) {
        if (!StringUtils.hasText(course.getName())) {
            throw BizException.badRequest("课程名称不能为空");
        }
        if (!StringUtils.hasText(course.getSubject())) {
            throw BizException.badRequest("科目不能为空");
        }

        if (course.getDurationMinutes() == null) {
            course.setDurationMinutes(45);
        }
        if (course.getDurationMinutes() <= 0) {
            throw BizException.badRequest("单次课时长必须大于 0");
        }

        if (course.getWeeklyTimes() == null) {
            course.setWeeklyTimes(1);
        }
        if (course.getWeeklyTimes() <= 0) {
            throw BizException.badRequest("周频次必须大于 0");
        }

        List<String> tags = course.getTags();
        if (tags != null && !TAGS.containsAll(tags)) {
            throw BizException.badRequest("课型标记只能是 TRIAL / MAKEUP / SUBSTITUTE / CROSS_CAMPUS");
        }
    }
}
