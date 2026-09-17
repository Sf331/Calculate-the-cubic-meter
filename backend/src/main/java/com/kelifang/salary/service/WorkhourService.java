package com.kelifang.salary.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kelifang.salary.entity.WorkhourRecord;
import com.kelifang.salary.mapper.WorkhourRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class WorkhourService extends ServiceImpl<WorkhourRecordMapper, WorkhourRecord> {

    /**
     * 记这节课的工时。
     *
     * 一节课一条：金额 = 固定课时单价 × 1 个教学课时。按节算而不是按学生算 ——
     * 5 个学生签到是上了同一节课，不能给老师算 5 份钱。
     *
     * upsert：一节课分两批点名（先点 3 个，下课前补点 2 个）时更新出勤人数，
     * 不会留下第二条工时。
     */
    public WorkhourRecord record(Long teacherId, Long scheduleId, Long attendanceId,
                                 LocalDate workDate, int studentCount, BigDecimal rate) {
        WorkhourRecord row = getOne(Wrappers.<WorkhourRecord>lambdaQuery()
                .eq(WorkhourRecord::getScheduleId, scheduleId));

        if (row == null) {
            row = new WorkhourRecord();
            row.setScheduleId(scheduleId);
        }
        row.setTeacherId(teacherId);
        row.setAttendanceId(attendanceId);
        row.setWorkDate(workDate);
        row.setStudentCount(studentCount);
        row.setRate(rate);
        row.setAmount(rate);
        saveOrUpdate(row);
        return row;
    }
}
