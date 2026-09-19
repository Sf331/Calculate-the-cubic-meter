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

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 内容托管的验收。和 {@link AcceptanceReconcileTest} 分开 —— 那套是课时/资金的对账，
 * 有 @Order 钉死的顺序和收尾；内容模块没有对账语义，混进去只会互相干扰。
 *
 * 同样全程走 HTTP，不注入任何 Service。上传那两条是**真的把字节取回来比**，
 * 不是看接口返回了个文件名就当成功了。
 *
 * 不需要排课，比签到那套快。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("内容托管验收")
class AcceptanceContentTest {

    @LocalServerPort
    int port;

    @Autowired
    ObjectMapper json;

    private AcceptanceApi principal;
    private AcceptanceApi teacher;
    private AcceptanceApi student;

    /** 校长建的「所有人可见」，后面几条拿它验可见范围 */
    private long publicAssetId;
    /** 教师建的「仅本人可见」，就是拿它验别人的 PRIVATE 看不看得到 */
    private long privateAssetId;

    @BeforeAll
    void login() {
        principal = AcceptanceApi.login(port, json, "principal");
        teacher = AcceptanceApi.login(port, json, "teacher01");
        student = AcceptanceApi.login(port, json, "student01");
    }

    /**
     * 文件没删，也没必要删 —— `./data/uploads` 是 demo 的落盘目录，
     * H2 每次重启重建但磁盘文件会留着。跑测试攒几个字节的垃圾，不值得为它写清理。
     */
    @AfterAll
    void cleanUp() {
        AcceptanceResetTest.resetAll(principal);
    }

    @Test
    @Order(1)
    @DisplayName("上传：文件真的落盘了，而且能从 /uploads/ 取回同样的字节")
    void 上传落盘且能取回() {
        byte[] content = "讲义内容：一次函数的图像".getBytes(StandardCharsets.UTF_8);

        String name = principal.postFile("/content/upload", "讲义.txt", content).require().asText();
        assertFalse(name.isBlank(), "上传应该返回一个文件名");

        // 光看接口返回不算数 —— 把字节取回来比
        assertArrayEquals(content, principal.getBytes("/uploads/" + name),
                "从 /uploads/ 取回来的应该是同一个文件");
    }

    @Test
    @Order(2)
    @DisplayName("上传：空文件被拒；学生不能往服务器上丢文件")
    void 上传的校验和权限() {
        assertEquals(400, principal.postFile("/content/upload", "空.txt", new byte[0]).code());
        assertEquals(403, student.postFile("/content/upload", "学生的.txt",
                "x".getBytes(StandardCharsets.UTF_8)).code(),
                "上传接口也要挡角色，否则谁都能往磁盘上丢东西");
    }

    @Test
    @Order(3)
    @DisplayName("新建：归属由后端按登录态填，不认请求体；版本从 1 起")
    void 新建时归属不被伪造() {
        Map<String, Object> body = asset("校长讲义", "HANDOUT", "ALL", 1L);
        body.put("ownerId", 999L);   // 故意伪造归属

        JsonNode created = principal.post("/content", body).require();
        publicAssetId = created.path("id").asLong();

        assertEquals(1, created.path("versionNo").asInt(), "版本应该从 1 起");
        assertNotEquals(999L, created.path("ownerId").asLong(),
                "请求体里的 ownerId 必须被无视，否则能伪造归属绕过可见范围");
        assertTrue(created.path("ownerId").asLong() > 0, "归属应该由后端填上");
    }

    @Test
    @Order(4)
    @DisplayName("按课次推送：选定课次后按它的课程过滤，别的课程的内容不出现")
    void 按课程过滤() {
        assertTrue(visibleIds(principal, 1L).contains(publicAssetId), "课程 1 的内容应该查得到");
        assertFalse(visibleIds(principal, 2L).contains(publicAssetId), "换一门课就不该出现");
    }

    @Test
    @Order(5)
    @DisplayName("可见范围：别人的 PRIVATE 看不到，自己的看得到")
    void 可见范围() {
        privateAssetId = teacher.post("/content", asset("教师私藏", "PAPER", "PRIVATE", 1L))
                .require().path("id").asLong();

        List<Long> seen = visibleIds(student, null);
        assertTrue(seen.contains(publicAssetId), "别人标了「所有人可见」的内容，学生该看得到");
        assertFalse(seen.contains(privateAssetId), "别人标了「仅本人可见」的内容，学生不该看到");
        assertTrue(visibleIds(teacher, null).contains(privateAssetId), "自己的私藏自己要看得到");
    }

    @Test
    @Order(6)
    @DisplayName("可见范围：按课次筛选时，自己挂在别的课上的私藏不该混进来")
    void 课程筛选不会把别课的内容混进来() {
        // 这条专门盯 Service 里 and(...or...) 那个括号。
        // 漏了括号 SQL 会变成 `course_id = ? AND scope = 'ALL' OR owner_id = ?`，
        // AND 结合更紧，于是 (course_id=? AND scope='ALL') OR (owner_id=我) ——
        // 只要是自己传的，不管挂在哪门课上都会被捞进来。
        // 所以这条必须拿「自己的、别的课的」内容来验；拿别人的内容验不出来。
        long otherCourse = teacher.post("/content", asset("别的课的私藏", "PAPER", "PRIVATE", 2L))
                .require().path("id").asLong();

        assertFalse(visibleIds(teacher, 1L).contains(otherCourse),
                "在课程 1 下面筛选，不该看到挂在课程 2 上的内容 —— 是 or 没加括号");
        assertTrue(visibleIds(teacher, null).contains(otherCourse),
                "不筛选的时候自己的内容还是该看得到");

        teacher.delete("/content/" + otherCourse);
    }

    @Test
    @Order(7)
    @DisplayName("版本：改一次加一，前端回传的旧版本号不作数")
    void 版本只递增() {
        Map<String, Object> body = asset("教师私藏", "PAPER", "PRIVATE", 1L);
        body.put("versionNo", 1);   // 前端把整行拷进表单，回传的就是上次的旧值

        assertEquals(2, teacher.put("/content/" + privateAssetId, body).require()
                .path("versionNo").asInt());
        assertEquals(3, teacher.put("/content/" + privateAssetId, body).require()
                .path("versionNo").asInt(), "第二次改应该是 3，说明每次都读库自算");
    }

    @Test
    @Order(8)
    @DisplayName("权限：改不了也删不了别人的；学生建不了内容；自己的能删")
    void 归属与角色() {
        assertEquals(403, teacher.put("/content/" + publicAssetId,
                asset("改别人的", "HANDOUT", "ALL", 1L)).code());
        assertEquals(403, teacher.delete("/content/" + publicAssetId).code());
        assertEquals(403, student.post("/content", asset("学生偷偷建", "HANDOUT", "ALL", null)).code(),
                "学生不该能建内容 —— 菜单藏了不算数，后端才是边界");

        assertEquals(0, teacher.delete("/content/" + privateAssetId).code());
        assertFalse(visibleIds(teacher, null).contains(privateAssetId), "删掉之后列表里不该还有");
    }

    @Test
    @Order(9)
    @DisplayName("入参校验：名称必填，类型和可见范围走白名单")
    void 入参校验() {
        assertEquals(400, principal.post("/content", asset("", "HANDOUT", "ALL", null)).code(),
                "名称必填");
        assertEquals(400, principal.post("/content", asset("x", "NOPE", "ALL", null)).code(),
                "类型不在白名单里");
        assertEquals(400, principal.post("/content", asset("x", "HANDOUT", "SUBJECT_GROUP", null)).code(),
                "SUBJECT_GROUP 这一档本版明确不做，不该被接受");
    }

    // ---------------- 小工具 ----------------

    /** 只看得到 id。courseId 传 null 就是不筛。 */
    private List<Long> visibleIds(AcceptanceApi api, Long courseId) {
        String path = courseId == null
                ? "/content/list?size=200"
                : "/content/list?size=200&courseId=" + courseId;
        return AcceptanceApi.ids(api.get(path).require().path("records"), "id");
    }

    /** 用 HashMap 而不是 Map.of：有些字段要缺省，Map.of 不收 null。 */
    private static Map<String, Object> asset(String name, String type, String scope, Long courseId) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("type", type);
        body.put("scope", scope);
        if (courseId != null) {
            body.put("courseId", courseId);
        }
        return body;
    }
}
