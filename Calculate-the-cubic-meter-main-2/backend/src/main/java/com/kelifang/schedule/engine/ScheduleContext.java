package com.kelifang.schedule.engine;

import com.kelifang.basedata.entity.Classroom;
import com.kelifang.basedata.entity.StudentConstraint;
import com.kelifang.basedata.entity.TeacherAvailability;

import java.util.List;
import java.util.Map;

/**
 * 求解引擎的全部输入。都是普通数据，不依赖数据库，方便单独测算法。
 *
 * 关于可用时段的约定：某个教师/学生在 map 里没有条目，或条目为空列表，都表示"不受限"。
 * 否则用户还没配约束数据时，一节课都排不出来。
 */
public record ScheduleContext(
        List<ClassTask> tasks,
        Map<Long, Classroom> classrooms,
        List<Classroom> allClassrooms,
        Map<Long, List<TeacherAvailability>> teacherWindows,
        Map<Long, List<StudentConstraint>> studentWindows,
        List<Booking> locked) {
}
