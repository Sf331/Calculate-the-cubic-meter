package com.kelifang.schedule.engine;

import com.kelifang.basedata.entity.Classroom;
import com.kelifang.basedata.entity.Clazz;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 构造阶段：贪心 + 约束最紧优先。
 *
 * 不是最优解，是"能跑出合法解"的解。计划书第五章说的优化阶段（模拟退火/禁忌搜索）
 * 放在 LocalSearchOptimizer 里，这一版先不做。
 */
public class GreedyScheduler {

    /** 候选时段。dateUsed 表示本周该班在这天已经排过课，排序时往后放。 */
    private record Candidate(LocalDate date, LocalTime start, boolean dateUsed) {
    }

    private record Placement(Long classroomId, LocalDate date, LocalTime start, LocalTime end) {
    }

    private static final int DEFAULT_DURATION_MINUTES = 45;
    private static final int MAX_REASONS_PER_CONFLICT = 3;

    private final ScheduleContext context;
    private final ConstraintValidator validator;

    public GreedyScheduler(ScheduleContext context) {
        this.context = context;
        this.validator = new ConstraintValidator(context);
    }

    public Result run(LocalDate startDate, int weeks) {
        List<Booking> placed = new ArrayList<>();
        Map<String, Conflict> conflicts = new LinkedHashMap<>();

        for (ClassTask task : orderByDifficulty(context.tasks())) {
            int duration = durationOf(task);
            int perWeek = perWeek(task);
            Set<LocalDate> usedDates = new HashSet<>();

            for (int week = 0; week < weeks; week++) {
                LocalDate weekStart = startDate.plusWeeks(week);

                for (int session = 0; session < perWeek; session++) {
                    List<String> failures = new ArrayList<>();
                    Placement placement = tryPlace(task,
                            buildCandidates(weekStart, duration, usedDates), duration, failures);

                    if (placement == null) {
                        recordConflict(conflicts, task, topReasons(failures));
                        continue;
                    }

                    Booking booking = new Booking(task.clazz().getId(), task.teacher().getId(),
                            placement.classroomId(), placement.date(), placement.start(),
                            placement.end(), task.studentIds());
                    validator.place(booking);
                    placed.add(booking);
                    usedDates.add(placement.date());
                }
            }
        }
        return new Result(placed, List.copyOf(conflicts.values()));
    }

    /** 同一个班因同样原因排不进去的多节课合并成一条，否则演示时一屏全是重复内容。 */
    private void recordConflict(Map<String, Conflict> conflicts, ClassTask task, List<String> reasons) {
        Long classId = task.clazz().getId();
        String key = classId + "|" + String.join("|", reasons);
        Conflict entry = new Conflict(classId, task.clazz().getName(), reasons, 1);
        conflicts.merge(key, entry, Conflict::merge);
    }

    /**
     * 约束最紧的先排。可用时段少的教师先安排，这是贪心排课最常用的启发式。
     * 没配可用时段的教师视为不受限，排最后。
     */
    private List<ClassTask> orderByDifficulty(List<ClassTask> tasks) {
        return tasks.stream()
                .sorted(Comparator
                        .comparingInt(this::windowCount)
                        .thenComparing(t -> t.students().size(), Comparator.reverseOrder()))
                .toList();
    }

    private int windowCount(ClassTask task) {
        List<?> windows = context.teacherWindows().get(task.teacher().getId());
        return (windows == null || windows.isEmpty()) ? Integer.MAX_VALUE : windows.size();
    }

    private List<Candidate> buildCandidates(LocalDate weekStart, int duration, Set<LocalDate> usedDates) {
        List<Candidate> candidates = new ArrayList<>();
        for (int day = 0; day < 7; day++) {
            LocalDate date = weekStart.plusDays(day);
            boolean used = usedDates.contains(date);
            for (LocalTime start : TeachingGrid.startTimes(date.getDayOfWeek(), duration)) {
                candidates.add(new Candidate(date, start, used));
            }
        }
        // 本周还没排过的日期优先，让同一门课尽量分散在不同天
        candidates.sort(Comparator.comparingInt(c -> c.dateUsed() ? 1 : 0));
        return candidates;
    }

    /** 逐个候选时段试，第一个通过全部硬约束的就用。全都失败则返回 null 并留下原因。 */
    private Placement tryPlace(ClassTask task, List<Candidate> candidates,
                               int duration, List<String> failures) {
        for (Candidate candidate : candidates) {
            LocalTime end = candidate.start().plusMinutes(duration);
            for (Long classroomId : candidateClassrooms(task)) {
                List<String> reasons =
                        validator.validate(task, classroomId, candidate.date(), candidate.start(), end);
                if (reasons.isEmpty()) {
                    return new Placement(classroomId, candidate.date(), candidate.start(), end);
                }
                failures.addAll(reasons);
            }
        }
        return null;
    }

    /** 班级指定了教室就用指定的，否则在本校区里找（容量够不够由校验器判断）。 */
    private List<Long> candidateClassrooms(ClassTask task) {
        Clazz clazz = task.clazz();
        if (clazz.getClassroomId() != null) {
            return List.of(clazz.getClassroomId());
        }
        return context.allClassrooms().stream()
                .filter(c -> clazz.getCampusId() == null || clazz.getCampusId().equals(c.getCampusId()))
                .map(Classroom::getId)
                .sorted()
                .toList();
    }

    /**
     * 按出现频率挑冲突原因。
     * 结构性原因（资质不匹配、指定教室容量不够）会在每个候选时段都出现，
     * 偶发原因（某个时段教室被占）只出现一两次。按频率排就能把真正的原因顶到前面。
     */
    private List<String> topReasons(List<String> failures) {
        return failures.stream()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed()
                        .thenComparing(Map.Entry::getKey))
                .limit(MAX_REASONS_PER_CONFLICT)
                .map(Map.Entry::getKey)
                .toList();
    }

    private static int durationOf(ClassTask task) {
        Integer minutes = task.course().getDurationMinutes();
        return (minutes == null || minutes <= 0) ? DEFAULT_DURATION_MINUTES : minutes;
    }

    private static int perWeek(ClassTask task) {
        Integer times = task.course().getWeeklyTimes();
        return (times == null || times <= 0) ? 1 : times;
    }

    public record Result(List<Booking> placed, List<Conflict> conflicts) {
    }
}
