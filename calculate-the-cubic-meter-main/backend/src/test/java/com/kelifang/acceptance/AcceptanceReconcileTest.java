package com.kelifang.acceptance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验收对账。计划书第九节第 3、4 条要的一致性断言就在这里。
 *
 * **对账的意思是不信服务自报。** 所以这里不注入任何 Service，全程走 HTTP
 * （{@link AcceptanceApi}），把账户、课时流水、资金流水分别拉下来自己重算一遍 ——
 * 接口返回的 "0 冲突"、"核销成功" 一个字都不作为依据。
 *
 * 用例之间有先后依赖（先开课、再点名、最后重算一遍），所以用 @Order 钉死顺序：
 * 第 14 条是收尾，在所有写操作跑完之后再验一次不变式 —— 前面全过了但这条不过，
 * 说明是哪次写操作把账写歪了。
 *
 * 跑的是自己 JVM 里的 H2 实例，不碰你 `mvn spring-boot:run` 那个库。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("验收对账")
class AcceptanceReconcileTest {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    /** 课表不是种子数据，是排课引擎排出来的。用 demo 那套同样的起点，第 1 周周一。 */
    private static final String START_DATE = "2026-09-14";
    private static final int WEEKS = 4;

    @LocalServerPort
    int port;

    @Autowired
    ObjectMapper json;

    private AcceptanceApi principal;
    private AcceptanceApi academic;
    private AcceptanceApi teacher;
    private AcceptanceApi student;
    private AcceptanceApi parent;

    /** 排课结果，@Order(0) 拿它断言 */
    private JsonNode generated;

    /** 收费开课开出来的学生，后面几条接着用 */
    private long rechargedStudentId;
    /** 点名用的课次和名单，「防重复核销」要拿它再点一次 */
    private long pointedScheduleId;
    private List<Map<String, Object>> pointedItems;
    /** 教师自己的 teacherId，「给别人的课点名」要拿它挑一节不属于自己的课 */
    private long ownTeacherId;

    @BeforeAll
    void login() {
        principal = AcceptanceApi.login(port, json, "principal");
        academic = AcceptanceApi.login(port, json, "academic");
        teacher = AcceptanceApi.login(port, json, "teacher01");
        student = AcceptanceApi.login(port, json, "student01");
        parent = AcceptanceApi.login(port, json, "parent01");

        // 前置：种子数据里没有课次，点名核销这一整条线要先有课表
        generated = principal.post("/schedule/generate",
                Map.<String, Object>of("startDate", START_DATE, "weeks", WEEKS)).require();
    }

    /** 收尾交给「重置验收」，把这个类造出来的临时学生和班级名单清掉。 */
    @AfterAll
    void reset() {
        AcceptanceResetTest.resetAll(principal);
    }

    // ---------------- 前置 ----------------

    @Test
    @Order(0)
    @DisplayName("排课：能排出合法课表，且一节都没报冲突")
    void 排课结果无冲突() {
        assertEquals(0, generated.path("conflictCount").asInt(),
                () -> "排课报了冲突：" + generated.path("conflicts"));
        assertTrue(generated.path("placedCount").asInt() > 0, "一节课都没排出来");
    }

    // ---------------- 一致性 ----------------

    @Test
    @Order(1)
    @DisplayName("验收3：每个账户的剩余课时 == 该账户全部课时流水之和")
    void 账户余额等于课时流水之和() {
        assertAccountsBalanced();
    }

    @Test
    @Order(2)
    @DisplayName("账户的已购/已耗课时 == 流水里 RECHARGE / CONSUME 的合计")
    void 已购已耗对得上流水() {
        for (JsonNode account : accounts()) {
            BigDecimal recharge = ZERO;
            BigDecimal consume = ZERO;
            for (JsonNode tx : transactions(account.path("id").asLong())) {
                BigDecimal hours = tx.path("hours").decimalValue();
                switch (tx.path("type").asText()) {
                    case "RECHARGE" -> recharge = recharge.add(hours);
                    case "CONSUME" -> consume = consume.add(hours.abs());
                    default -> { }
                }
            }
            assertSameAmount(label(account) + " 已购课时", recharge,
                    account.path("totalHours").decimalValue());
            assertSameAmount(label(account) + " 已消耗课时", consume,
                    account.path("consumedHours").decimalValue());
        }
    }

    @Test
    @Order(3)
    @DisplayName("课时单价 == 该账户已收预收总额 / 已购课时（口径独立重算）")
    void 课时单价独立重算一致() {
        assertUnitPricesConsistent();
    }

    // ---------------- 收费开课 ----------------

    @Test
    @Order(4)
    @DisplayName("收费开课：本来没账户的学生，收完费当场就有账户")
    void 新学生收费开课() {
        JsonNode temp = principal.post("/basedata/student",
                Map.<String, Object>of("name", AcceptanceResetTest.TEMP_PREFIX + "乙", "grade", "三年级"))
                .require();
        rechargedStudentId = temp.path("id").asLong();

        assertEquals(0, principal.get("/lesson-account?studentId=" + rechargedStudentId)
                .require().size(), "新学生本来就不该有课时账户");

        JsonNode account = principal.post("/lesson-account/recharge",
                recharge(rechargedStudentId, 1, "40", "4800")).require();

        assertSameAmount("已购课时", new BigDecimal("40"), account.path("totalHours").decimalValue());
        assertSameAmount("剩余课时", new BigDecimal("40"), account.path("remainingHours").decimalValue());
        assertSameAmount("单价 = 4800 / 40", new BigDecimal("120"),
                account.path("unitPrice").decimalValue());
    }

    @Test
    @Order(5)
    @DisplayName("同一账户追加充值：课时累加，单价不动")
    void 追加充值单价不变() {
        JsonNode account = principal.post("/lesson-account/recharge",
                recharge(rechargedStudentId, 1, "20", "2400")).require();

        assertSameAmount("累计已购", new BigDecimal("60"), account.path("totalHours").decimalValue());
        assertSameAmount("累计剩余", new BigDecimal("60"), account.path("remainingHours").decimalValue());
        assertSameAmount("单价还是 7200 / 60", new BigDecimal("120"),
                account.path("unitPrice").decimalValue());

        // 充值也是写路径，余额==流水之和不能破
        assertAccountsBalanced();
    }

    // ---------------- 点名核销 ----------------

    @Test
    @Order(6)
    @DisplayName("一次点名：课时流水、资金流水同时变，三份账仍然平")
    void 一次点名三处联动() {
        JsonNode lessons = principal.get("/attendance/lessons").require();
        assertTrue(lessons.size() > 0, "种子数据应该有课次");
        pointedScheduleId = lessons.get(0).path("id").asLong();

        JsonNode session = principal.get("/attendance/" + pointedScheduleId).require();
        pointedItems = new ArrayList<>();
        for (JsonNode s : session.path("students")) {
            pointedItems.add(Map.<String, Object>of(
                    "studentId", s.path("studentId").asLong(), "status", "PRESENT"));
        }
        assertFalse(pointedItems.isEmpty(), "这节课应该有人可以点");

        JsonNode result = principal.post("/attendance/" + pointedScheduleId,
                Map.<String, Object>of("items", pointedItems)).require();

        assertEquals(pointedItems.size(), result.path("attendanceCount").asInt(), "签到人数");
        // 出勤每人扣 1 课时，不按人头算的是老师的工时
        assertSameAmount("本次消耗课时", BigDecimal.valueOf(pointedItems.size()),
                result.path("consumedHours").decimalValue());
        assertTrue(result.path("confirmedAmount").decimalValue().signum() > 0, "确认收入应该大于 0");
        assertTrue(result.path("workhourAmount").decimalValue().signum() > 0, "教师工时应该大于 0");

        assertAccountsBalanced();
        assertUnitPricesConsistent();
        assertRevenueBalanced();
    }

    @Test
    @Order(7)
    @DisplayName("防重复核销：同一节课同一学生再点一次被拒，且报错说得清原因")
    void 重复核销被拒() {
        AcceptanceApi.Resp resp = principal.post("/attendance/" + pointedScheduleId,
                Map.<String, Object>of("items", pointedItems));

        assertEquals(400, resp.code());
        assertTrue(resp.msg().contains("已经点过名"), () -> "报错要说清原因，实际：" + resp.msg());
    }

    // ---------------- 事务 ----------------

    @Test
    @Order(8)
    @DisplayName("事务：一批里混进没有课时账户的学生，整批都不落库")
    void 混进没账户的学生整批回滚() {
        JsonNode lesson = anotherLesson();
        long scheduleId = lesson.path("id").asLong();
        long classId = principal.get("/attendance/" + scheduleId).require().path("classId").asLong();

        JsonNode temp = principal.post("/basedata/student",
                Map.<String, Object>of("name", AcceptanceResetTest.TEMP_PREFIX + "丙", "grade", "三年级"))
                .require();
        long tempId = temp.path("id").asLong();

        List<Long> roster = AcceptanceApi.ids(
                principal.get("/basedata/clazz/" + classId + "/student").require(), "id");
        roster.add(tempId);
        principal.put("/basedata/clazz/" + classId + "/student", roster).require();

        // 把没有账户的临时学生排在最后：前面的学生会被真写进去，再跟着一起回滚
        List<Map<String, Object>> items = new ArrayList<>();
        for (long id : roster) {
            if (id != tempId) {
                items.add(Map.<String, Object>of("studentId", id, "status", "PRESENT"));
            }
        }
        items.add(Map.<String, Object>of("studentId", tempId, "status", "PRESENT"));

        AcceptanceApi.Resp resp = principal.post("/attendance/" + scheduleId,
                Map.<String, Object>of("items", items));

        assertEquals(400, resp.code(), () -> "应该因为有人没课时账户而失败，实际：" + resp.text());
        assertTrue(resp.msg().contains("课时账户"), () -> "报错要说清是谁卡住了，实际：" + resp.msg());

        // 整批回滚的判据：这节课上一个人都没签到
        for (JsonNode s : principal.get("/attendance/" + scheduleId).require().path("students")) {
            assertTrue(s.path("status").isNull(),
                    () -> s.path("studentName").asText() + " 不该留下签到记录 —— 整批应该回滚");
        }
    }

    // ---------------- 权限与范围 ----------------

    @Test
    @Order(9)
    @DisplayName("收费权限：学生、家长、教师一律 403，教务可以")
    void 收费权限() {
        Map<String, Object> body = recharge(1L, 1, "10", "1000");
        for (AcceptanceApi api : List.of(student, parent, teacher)) {
            assertEquals(403, api.post("/lesson-account/recharge", body).code());
        }

        JsonNode account = academic.post("/lesson-account/recharge",
                recharge(rechargedStudentId, 2, "10", "1300")).require();
        assertEquals(2, account.path("courseId").asLong(), "教务给这个学生开了第二门课的账户");
        assertSameAmount("教务充的值", new BigDecimal("130"),
                account.path("unitPrice").decimalValue());
    }

    @Test
    @Order(10)
    @DisplayName("家长范围：指定别人家孩子的 studentId 也只能拿到自己的孩子")
    void 家长拿不到别人家孩子() {
        JsonNode mine = parent.get("/lesson-account").require();
        assertFalse(mine.isEmpty(), "parent01 应该看得到小明");
        long ownChild = mine.get(0).path("studentId").asLong();
        for (JsonNode a : mine) {
            assertEquals(ownChild, a.path("studentId").asLong());
        }

        for (JsonNode a : parent.get("/lesson-account?studentId=2").require()) {
            assertEquals(ownChild, a.path("studentId").asLong(),
                    "家长指定了别人家的孩子，后端不该认这个参数");
        }
    }

    @Test
    @Order(11)
    @DisplayName("课次范围：教师只拿得到自己的课")
    void 教师只拿得到自己的课次() {
        JsonNode all = principal.get("/attendance/lessons").require();
        JsonNode mine = teacher.get("/attendance/lessons").require();
        assertFalse(mine.isEmpty(), "teacher01 应该有课");

        Set<Long> allIds = new HashSet<>(AcceptanceApi.ids(all, "id"));
        Set<Long> teachers = new HashSet<>();
        for (JsonNode lesson : mine) {
            teachers.add(lesson.path("teacherId").asLong());
            assertTrue(allIds.contains(lesson.path("id").asLong()),
                    "教师的课必须是全量课表的子集");
        }

        assertEquals(1, teachers.size(), "教师只该看到自己一个老师的课");
        assertTrue(mine.size() < all.size(), "教师拿到的课次应该少于校长看到的全部");
        ownTeacherId = teachers.iterator().next();
    }

    @Test
    @Order(12)
    @DisplayName("点名权限：教师给别人的课点名被拒")
    void 教师给别人的课点名被拒() {
        JsonNode foreign = null;
        for (JsonNode lesson : principal.get("/attendance/lessons").require()) {
            if (lesson.path("teacherId").asLong() != ownTeacherId) {
                foreign = lesson;
                break;
            }
        }
        assertNotNull(foreign, "需要一节不属于 teacher01 的课");

        AcceptanceApi.Resp resp = teacher.post("/attendance/" + foreign.path("id").asLong(),
                Map.<String, Object>of("items",
                        List.of(Map.<String, Object>of("studentId", 1L, "status", "PRESENT"))));

        assertEquals(403, resp.code());
    }

    @Test
    @Order(13)
    @DisplayName("学生范围：只看到自己的课时账户")
    void 学生只看到自己的账户() {
        JsonNode mine = student.get("/lesson-account").require();
        assertFalse(mine.isEmpty(), "student01 应该有账户");
        long own = mine.get(0).path("studentId").asLong();
        for (JsonNode a : mine) {
            assertEquals(own, a.path("studentId").asLong());
        }

        for (JsonNode a : student.get("/lesson-account?studentId=2").require()) {
            assertEquals(own, a.path("studentId").asLong());
        }
    }

    // ---------------- 收尾 ----------------

    @Test
    @Order(14)
    @DisplayName("收尾：所有写操作跑完之后，三份账仍然是平的")
    void 全部写操作之后账仍然平() {
        assertAccountsBalanced();
        assertUnitPricesConsistent();
        assertRevenueBalanced();
    }

    // ---------------- 断言实现 ----------------

    /** 验收 3：余额等于该账户全部流水之和。账户余额是冗余列，它一旦漂了，整个课时体系就是假的。 */
    private void assertAccountsBalanced() {
        JsonNode rows = accounts();
        assertTrue(rows.size() > 0, "种子数据应该有课时账户");
        for (JsonNode account : rows) {
            BigDecimal remaining = account.path("remainingHours").decimalValue();
            BigDecimal sum = ZERO;
            for (JsonNode tx : transactions(account.path("id").asLong())) {
                sum = sum.add(tx.path("hours").decimalValue());
            }
            assertSameAmount(label(account) + " 剩余课时", sum, remaining);
        }
    }

    /** 单价 = 已收预收 / 已购课时。这里不读接口返回的单价，自己从资金流水重算一遍再比。 */
    private void assertUnitPricesConsistent() {
        Map<Long, BigDecimal> paid = paidByAccount();
        for (JsonNode account : accounts()) {
            BigDecimal bought = account.path("totalHours").decimalValue();
            BigDecimal received = paid.getOrDefault(account.path("id").asLong(), ZERO);
            BigDecimal expected = bought.signum() == 0
                    ? ZERO
                    : received.divide(bought, 2, RoundingMode.HALF_UP);
            assertSameAmount(label(account) + " 课时单价", expected,
                    account.path("unitPrice").decimalValue());
        }
    }

    /**
     * 验收 4：消耗课时冲掉的预收（OUT）和确认的收入（IN）必须一一对上。
     * 两者都挂在 attendance.id 上，所以按 ref_id 配对 —— 配不上的 ref_id 本身就是问题。
     */
    private void assertRevenueBalanced() {
        Map<Long, BigDecimal> preReceiveOut = new HashMap<>();
        Map<Long, BigDecimal> confirmed = new HashMap<>();

        for (JsonNode row : principal.get("/finance/transaction").require()) {
            String type = row.path("type").asText();
            long refId = row.path("refId").asLong();
            BigDecimal amount = row.path("amount").decimalValue();
            if ("PRE_RECEIVE".equals(type) && "OUT".equals(row.path("direction").asText())) {
                preReceiveOut.merge(refId, amount, BigDecimal::add);
            } else if ("RECEIVE_CONFIRM".equals(type)) {
                confirmed.merge(refId, amount, BigDecimal::add);
            }
        }

        assertFalse(preReceiveOut.isEmpty(), "点过名就该有冲预收的流水");
        assertEquals(preReceiveOut.keySet(), confirmed.keySet(),
                "每一笔冲预收都该有对应的收入确认，反过来也一样");
        for (Map.Entry<Long, BigDecimal> entry : preReceiveOut.entrySet()) {
            assertSameAmount("attendance#" + entry.getKey() + " 冲预收 vs 确认收入",
                    entry.getValue(), confirmed.get(entry.getKey()));
        }
    }

    // ---------------- 小工具 ----------------

    private JsonNode accounts() {
        return principal.get("/lesson-account").require();
    }

    private JsonNode transactions(long accountId) {
        return principal.get("/lesson-account/" + accountId + "/transaction").require();
    }

    /** 预收 IN 的 ref_id 指向 lesson_account.id，按它汇总就是"这个账户一共收过多少钱"。 */
    private Map<Long, BigDecimal> paidByAccount() {
        Map<Long, BigDecimal> paid = new HashMap<>();
        for (JsonNode row : principal.get("/finance/transaction").require()) {
            if ("PRE_RECEIVE".equals(row.path("type").asText())
                    && "IN".equals(row.path("direction").asText())) {
                paid.merge(row.path("refId").asLong(), row.path("amount").decimalValue(),
                        BigDecimal::add);
            }
        }
        return paid;
    }

    /** 找一节不是刚才点过的那节，用来验回滚，免得和防重复核销撞在一起。 */
    private JsonNode anotherLesson() {
        for (JsonNode lesson : principal.get("/attendance/lessons").require()) {
            if (lesson.path("id").asLong() != pointedScheduleId) {
                return lesson;
            }
        }
        throw new AssertionError("至少要有两节课才能验事务回滚");
    }

    private Map<String, Object> recharge(long studentId, long courseId, String hours, String amount) {
        return Map.of("studentId", studentId, "courseId", courseId,
                "hours", new BigDecimal(hours), "amount", new BigDecimal(amount));
    }

    private String label(JsonNode account) {
        return account.path("studentName").asText() + " / " + account.path("courseName").asText();
    }

    /** 用 compareTo 比大小，不用 equals —— BigDecimal 的 40 和 40.00 不是一个 equals 但相等。 */
    private static void assertSameAmount(String what, BigDecimal expected, BigDecimal actual) {
        assertEquals(0, expected.compareTo(actual),
                () -> what + "：期望 " + expected + "，实际 " + actual);
    }
}
