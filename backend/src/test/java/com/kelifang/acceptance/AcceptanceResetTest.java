package com.kelifang.acceptance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 重置验收：把验收过程造出来的数据清掉，让对账可以在干净的基础上重跑。
 *
 * `resetAll` 是给 {@link AcceptanceReconcileTest} 收尾用的 —— 它跑完会留下一两个
 * 为验"整批回滚"临时塞进班级的学生。这个类自己也有一个测试，验重置本身是对的。
 *
 * **注意作用域**：{@code @SpringBootTest} 跑在自己的 JVM 里，用的是自己那份
 * `jdbc:h2:mem:kelifang`，和你 `mvn spring-boot:run` 演示的那个库不是同一个
 * （H2 内存库按 JVM 隔离）。所以重置清的不是演示库，是测试进程里的库 ——
 * 演示库压根没被碰过。以前那份 Python 脚本才是真会污染演示库的。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AcceptanceResetTest {

    /** 验收临时造的学生的名字前缀。重置靠它认人，别改成一个正常学生会用的名字。 */
    public static final String TEMP_PREFIX = "验收临时生";

    @LocalServerPort
    int port;

    @Autowired
    ObjectMapper json;

    private AcceptanceApi principal;

    @BeforeAll
    void setUp() {
        principal = AcceptanceApi.login(port, json, "principal");
    }

    @Test
    @DisplayName("重置：验下来的临时学生和他挂在班级名单里的那一行一起清掉")
    void 重置把验收造的数据清干净() {
        JsonNode temp = principal.post("/basedata/student",
                Map.of("name", TEMP_PREFIX + "甲", "grade", "三年级")).require();
        long tempId = temp.path("id").asLong();

        List<Long> roster = AcceptanceApi.ids(roster(1), "id");
        roster.add(tempId);
        principal.put("/basedata/clazz/1/student", roster).require();
        assertTrue(AcceptanceApi.ids(roster(1), "id").contains(tempId),
                "刚塞进去的临时学生应该在名单里");

        resetAll(principal);

        assertFalse(AcceptanceApi.ids(aliveStudents(), "id").contains(tempId),
                "重置之后临时学生应该已经不在学生表里了");
        assertFalse(AcceptanceApi.ids(roster(1), "id").contains(tempId),
                "重置之后名单里不该再挂着这个学生");
    }

    /**
     * 清掉验收留下的痕迹。断言类跑完调它。
     *
     * 两步：删掉名字带 {@link #TEMP_PREFIX} 的学生；再把各班名单里指向"已经不存在的学生"
     * 的行剔掉。第二步是按存活学生反查的，不靠事先记下改过哪个班 ——
     * 这样任何测试往任何班里塞人都能收干净，不用各自登记。
     */
    static void resetAll(AcceptanceApi principal) {
        for (JsonNode student : principal.get("/basedata/student?page=1&size=500").require()
                .path("records")) {
            if (student.path("name").asText().startsWith(TEMP_PREFIX)) {
                principal.delete("/basedata/student/" + student.path("id").asLong()).require();
            }
        }

        List<Long> alive = AcceptanceApi.ids(aliveStudents(principal), "id");
        for (JsonNode clazz : principal.get("/basedata/clazz?page=1&size=100").require()
                .path("records")) {
            long classId = clazz.path("id").asLong();
            List<Long> roster = AcceptanceApi.ids(roster(principal, classId), "id");
            List<Long> cleaned = roster.stream().filter(alive::contains).toList();
            if (cleaned.size() != roster.size()) {
                principal.put("/basedata/clazz/" + classId + "/student", cleaned).require();
            }
        }
    }

    private JsonNode aliveStudents() {
        return aliveStudents(principal);
    }

    private JsonNode roster(long classId) {
        return roster(principal, classId);
    }

    private static JsonNode aliveStudents(AcceptanceApi principal) {
        return principal.get("/basedata/student?page=1&size=500").require().path("records");
    }

    private static JsonNode roster(AcceptanceApi principal, long classId) {
        return principal.get("/basedata/clazz/" + classId + "/student").require();
    }
}
