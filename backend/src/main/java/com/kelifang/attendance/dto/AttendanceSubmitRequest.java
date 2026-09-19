package com.kelifang.attendance.dto;

import lombok.Data;

import java.util.List;

/** 一次点名提交的是整张名单上被改动过的那部分。 */
@Data
public class AttendanceSubmitRequest {

    private List<AttendanceItem> items;
}
