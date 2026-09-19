package com.kelifang.schedule.engine;

import com.kelifang.basedata.entity.Classroom;
import com.kelifang.basedata.entity.Clazz;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 增量重排。
 *
 * 和贪心整表重排的区别：只动"被挤到的"那几节课，没受影响的课表片段原样保留。
 * 这对应计划书"锁定未受影响课表片段，仅对受影响的子问题重新求解"。
 *
 * 策略是"搬走—重放"：目标课先占住新时段，把挡路的课逐个挪到别处。
 * 任何一步走不通就整体放弃并说明原因 —— 不做半截方案，否则教务看到的就是一张坏课表。
 */
public class IncrementalRescheduler {

    /** 一次挪动：从哪到哪。 */
    public record Move(Placement from, Placement to) {
    }

    /** feasible = false 时 moves 为空、reasons 说明为什么挪不动。 */
    public record Result(boolean feasible, List<Move> moves, List<String> reasons) {

        static Result fail(String reason) {
            return new Result(false, List.of(), List.of(reason));
        }
    }

    /** 被挤走的课允许挪到原位置前后各一周以内，超出这个范围就不叫"增量"了 */
    private static final int NEIGHBOUR_WEEKS = 1;

    private final ScheduleContext context;
    private final Map<Long, Placement> byId = new LinkedHashMap<>();
    private final Map<Long, ClassTask> taskByClass = new HashMap<>();

    public IncrementalRescheduler(ScheduleContext context, List<Placement> placements) {
        this.context = context;
        placements.forEach(p -> byId.put(p.scheduleId(), p));
        context.tasks().forEach(t -> taskByClass.put(t.clazz().getId(), t));
    }

    public Result resolve(Long targetId, LocalDate date, LocalTime start, Long classroomId) {
        Placement target = byId.get(targetId);
        if (target == null) {
            return Result.fail("这节课不存在");
        }
        ClassTask task = taskByClass.get(target.classId());
        if (task == null) {
            return Result.fail("这节课所属的班级已被删除");
        }

        Placement desired = target.movedTo(date, start,
                start.plusMinutes(durationMinutes(task)),
                classroomId != null ? classroomId : target.classroomId());

        List<Placement> others = byId.values().stream()
                .filter(p -> !p.scheduleId().equals(targetId))
                .toList();

        // 目标时段本身是否合法。占用集传空 —— 撞到别人是可以靠挪开解决的，
        // 但教学网格、教师/学生可用时段、容量、资质挪开谁都没用，这些必须一开始就拦掉。
        List<String> structural = validateInto(List.of(), task, desired.classroomId(),
                desired.date(), desired.start(), desired.end());
        if (!structural.isEmpty()) {
            return new Result(false, List.of(), structural);
        }

        List<Placement> blockers = others.stream().filter(p -> p.clashesWith(desired)).toList();
        for (Placement blocker : blockers) {
            if (blocker.locked()) {
                return Result.fail("「" + label(blocker) + "」已锁定，正占用目标时段，无法自动调整");
            }
        }

        // 目标先占住新位置，挡路的那些从占用集里摘出来，逐个另找位置
        List<Placement> occupied = new ArrayList<>(others);
        occupied.removeAll(blockers);
        occupied.add(desired);

        List<Move> moves = new ArrayList<>();
        moves.add(new Move(target, desired));

        for (Placement blocker : blockers) {
            Placement spot = findSlot(blocker, occupied);
            if (spot == null) {
                return Result.fail("「" + label(blocker) + "」在前后一周内找不到可挪的时段，本次调整放弃");
            }
            moves.add(new Move(blocker, spot));
            occupied.add(spot);
        }
        return new Result(true, moves, List.of());
    }

    // ---- 给一节被挤走的课找新位置 ----

    private Placement findSlot(Placement placement, List<Placement> occupied) {
        ClassTask task = taskByClass.get(placement.classId());
        if (task == null) {
            return null;
        }

        int duration = (int) Duration.between(placement.start(), placement.end()).toMinutes();
        if (duration <= 0) {
            duration = durationMinutes(task);
        }

        List<Placement> others = occupied.stream()
                .filter(o -> !o.scheduleId().equals(placement.scheduleId()))
                .toList();
        Map<LocalDate, Long> load = others.stream()
                .collect(Collectors.groupingBy(Placement::date, Collectors.counting()));

        for (Candidate candidate : candidateSlots(placement, duration, load)) {
            LocalTime end = candidate.start().plusMinutes(duration);
            for (Long roomId : roomsFor(task, placement.classroomId())) {
                List<String> reasons = validateInto(others, task, roomId,
                        candidate.date(), candidate.start(), end);
                if (reasons.isEmpty()) {
                    return placement.movedTo(candidate.date(), candidate.start(), end, roomId);
                }
            }
        }
        return null;
    }

    private record Candidate(LocalDate date, LocalTime start, long dayDistance, long load,
                             boolean originalSlot) {
    }

    /**
     * 候选时段的排序决定了"扰动有多大"：
     * 原位置最优，其次离原位置越近越好，再次当天负载越小越好。
     * 就近优先是刻意的 —— 调一次课把别的课甩到另一周去，教务要疯。
     */
    private List<Candidate> candidateSlots(Placement placement, int duration,
                                           Map<LocalDate, Long> load) {
        LocalDate weekStart = placement.date().with(DayOfWeek.MONDAY);
        List<Candidate> candidates = new ArrayList<>();

        for (int week = -NEIGHBOUR_WEEKS; week <= NEIGHBOUR_WEEKS; week++) {
            for (int day = 0; day < 7; day++) {
                LocalDate date = weekStart.plusWeeks(week).plusDays(day);
                boolean isOriginalDay = date.equals(placement.date());
                long distance = Math.abs(ChronoUnit.DAYS.between(placement.date(), date));

                for (LocalTime start : TeachingGrid.startTimes(date.getDayOfWeek(), duration)) {
                    boolean original = isOriginalDay && start.equals(placement.start());
                    candidates.add(new Candidate(date, start, distance,
                            load.getOrDefault(date, 0L), original));
                }
            }
        }

        candidates.sort(Comparator
                .comparing((Candidate c) -> !c.originalSlot())
                .thenComparingLong(Candidate::dayDistance)
                .thenComparingLong(Candidate::load)
                .thenComparing(Candidate::date)
                .thenComparing(Candidate::start));
        return candidates;
    }

    /** 原教室优先，其次本校区其他教室。班级指定了教室就不能换。 */
    private List<Long> roomsFor(ClassTask task, Long originalRoomId) {
        Clazz clazz = task.clazz();
        if (clazz.getClassroomId() != null) {
            return List.of(clazz.getClassroomId());
        }

        List<Long> roomIds = new ArrayList<>();
        if (originalRoomId != null) {
            roomIds.add(originalRoomId);
        }
        context.allClassrooms().stream()
                .filter(c -> clazz.getCampusId() == null || clazz.getCampusId().equals(c.getCampusId()))
                .map(Classroom::getId)
                .sorted()
                .filter(id -> !roomIds.contains(id))
                .forEach(roomIds::add);
        return roomIds;
    }

    private List<String> validateInto(List<Placement> others, ClassTask task, Long roomId,
                                      LocalDate date, LocalTime start, LocalTime end) {
        ConstraintValidator validator = new ConstraintValidator(context,
                others.stream().map(Placement::toBooking).toList());
        return validator.validate(task, roomId, date, start, end);
    }

    private String label(Placement placement) {
        ClassTask task = taskByClass.get(placement.classId());
        return task == null ? "班级" + placement.classId() : task.clazz().getName();
    }

    private static int durationMinutes(ClassTask task) {
        Integer minutes = task.course().getDurationMinutes();
        return (minutes == null || minutes <= 0) ? 45 : minutes;
    }
}
