package com.kelifang.schedule.vo;

import com.kelifang.schedule.engine.Conflict;

import java.util.List;

/** 一次排课的结果。conflicts 直接给前端弹窗展示"哪些班排不进去、为什么"。 */
public record GenerateResult(int placedCount, int conflictCount, List<Conflict> conflicts) {

    public static GenerateResult of(int placedCount, List<Conflict> conflicts) {
        return new GenerateResult(placedCount, conflicts.size(), conflicts);
    }
}
