package com.kelifang.schedule.engine;

import java.util.List;

/**
 * 求解失败时留下的冲突记录。
 * sessionCount 是落在同一个原因上的课时数，避免一个班报 4 条一模一样的内容。
 */

//record 是 Java 14 引入的特性，表示一个不可变的数据类，
// 自动生成构造函数、getter、equals、hashCode 和 toString 方法。
public record Conflict(Long classId, String className, List<String> reasons, int sessionCount) {

    public Conflict merge(Conflict other) {
        return new Conflict(classId, className, reasons, sessionCount + other.sessionCount);
    }
}
