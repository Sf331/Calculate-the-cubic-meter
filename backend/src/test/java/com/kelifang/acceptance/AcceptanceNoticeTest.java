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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 站内通知的验收。全程走 HTTP，不注入 Service —— 和别的验收一样，
 * 这里要验的是「发布方发出去，另一头真的看得见、看不见」这条边界。
 *
 * **通知存在内存里、不落库**，所以 {@link AcceptanceResetTest} 那套重置清不掉它：
 * 本类自己在前后各清一次，免得和别的测试类共用 Spring 上下文时互相看见对方造的数据。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("站内通知验收")
class AcceptanceNoticeTest {

    @LocalServerPort
    int port;

    @Autowired
    ObjectMapper json;

    private AcceptanceApi principal;
    private AcceptanceApi academic;
    private AcceptanceApi teacher;
    private AcceptanceApi student;
    private AcceptanceApi parent;

    @BeforeAll
    void setUp() {
        principal = AcceptanceApi.login(port, json, "principal");
        academic = AcceptanceApi.login(port, json, "academic");
        teacher = AcceptanceApi.login(port, json, "teacher01");
        student = AcceptanceApi.login(port, json, "student01");
        parent = AcceptanceApi.login(port, json, "parent01");
        clearNotices();
    }

    @AfterAll
    void cleanUp() {
        clearNotices();
    }

    @Test
    @Order(1)
    @DisplayName("空态：一条通知都没有时，学生和家长拿到的是空列表（前端显示「无通知」）")
    void 没有通知时是空列表() {
        assertEquals(0, list(student).size(), "数据全在内存里，刚起来就该是空的");
        assertEquals(0, list(parent).size());
    }

    @Test
    @Order(2)
    @DisplayName("发布：发布者和发布时间由后端按登录态填，伪造不了；面向全体的学生和家长都看得到")
    void 发布的归属由后端填() {
        Map<String, Object> body = notice("国庆假期停课安排", "ALL");
        // 前端理论上不会传这两个字段，传了也必须被无视
        body.put("publisherName", "假名字");
        body.put("publisherId", 999L);

        JsonNode created = teacher.post("/notice", body).require();
        assertEquals("李老师", created.path("publisherName").asText(), "发布者必须是登录的那个人");
        assertNotEquals(999L, created.path("publisherId").asLong(), "请求体里的发布者字段不能作数");
        assertFalse(created.path("createdAt").asText().isBlank(), "发布时间应该由后端填上");

        assertEquals(1, list(student).size(), "面向全体的通知，学生该看得到");
        assertEquals(1, list(parent).size(), "面向全体的通知，家长也该看得到");
        assertEquals(1, list(academic).size(), "发布页要能看到已发布的全部通知");
    }

    @Test
    @Order(3)
    @DisplayName("面向：仅学生的通知家长看不到，仅家长的通知学生看不到，新的排在最前")
    void 面向决定谁看得到() {
        teacher.post("/notice", notice("仅学生：月考安排", "STUDENT")).require();
        teacher.post("/notice", notice("仅家长：家长会改期", "PARENT")).require();

        List<String> studentTitles = titles(list(student));
        List<String> parentTitles = titles(list(parent));

        assertTrue(studentTitles.contains("仅学生：月考安排"), "面向学生的通知学生该看到");
        assertFalse(studentTitles.contains("仅家长：家长会改期"), "面向家长的通知不该漏给学生");
        assertTrue(parentTitles.contains("仅家长：家长会改期"), "面向家长的通知家长该看到");
        assertFalse(parentTitles.contains("仅学生：月考安排"), "面向学生的通知不该漏给家长");

        assertEquals("仅学生：月考安排", studentTitles.get(0), "刚发的应该排在最前");
    }

    @Test
    @Order(4)
    @DisplayName("撤回：撤回之后学生和家长都看不到了")
    void 撤回之后收方看不到了() {
        long id = academic.post("/notice", notice("临时停课一次", "ALL")).require()
                .path("id").asLong();
        assertTrue(ids(list(student)).contains(id), "刚发布的应该看得到");

        assertEquals(0, academic.delete("/notice/" + id).code());
        assertFalse(ids(list(student)).contains(id), "撤回之后学生不该再看到");
        assertFalse(ids(list(parent)).contains(id), "撤回之后家长不该再看到");
    }

    @Test
    @Order(5)
    @DisplayName("边界：学生和家长发不了通知，空标题和非法面向被拒")
    void 权限与校验() {
        assertEquals(403, student.post("/notice", notice("学生偷发", "ALL")).code(),
                "菜单藏了不算数，后端才是边界");
        assertEquals(403, parent.post("/notice", notice("家长偷发", "ALL")).code());
        assertEquals(403, student.delete("/notice/1").code(), "撤回同样只给发布方");

        assertEquals(400, teacher.post("/notice", notice("   ", "ALL")).code(), "空标题要拒掉");
        assertEquals(400, teacher.post("/notice", notice("面向写错", "TEACHER")).code(),
                "通知只能发给学生和家长");
    }

    /** 通知不落库，重置接口清不掉它，所以本类自己收尾。 */
    private void clearNotices() {
        for (long id : ids(list(principal))) {
            principal.delete("/notice/" + id);
        }
    }

    private JsonNode list(AcceptanceApi api) {
        return api.get("/notice/list").require();
    }

    private static List<Long> ids(JsonNode array) {
        return AcceptanceApi.ids(array, "id");
    }

    private static List<String> titles(JsonNode array) {
        List<String> result = new ArrayList<>();
        for (JsonNode node : array) {
            result.add(node.path("title").asText());
        }
        return result;
    }

    private static Map<String, Object> notice(String title, String audience) {
        Map<String, Object> body = new HashMap<>();
        body.put("title", title);
        body.put("content", "演示内容");
        body.put("audience", audience);
        return body;
    }
}
