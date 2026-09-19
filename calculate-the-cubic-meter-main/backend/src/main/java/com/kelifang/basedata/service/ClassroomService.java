package com.kelifang.basedata.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kelifang.basedata.entity.Classroom;
import com.kelifang.basedata.mapper.ClassroomMapper;
import com.kelifang.common.BizException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ClassroomService extends ServiceImpl<ClassroomMapper, Classroom> {

    public void check(Classroom classroom) {
        if (!StringUtils.hasText(classroom.getName())) {
            throw BizException.badRequest("教室名称不能为空");
        }
        if (classroom.getCapacity() == null || classroom.getCapacity() < 0) {
            throw BizException.badRequest("教室容量必须是不小于 0 的整数");
        }
    }
}
