package com.kelifang.acceptance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 验收测试的 HTTP 客户端：走真实端口、真实 cookie 会话。
 *
 * **刻意不注入 Service。** 对账的意思是把数据从接口拉下来自己重算，
 * 而不是问业务代码"你对不对"。同一个 JVM 里直调 Service 会把这条界线抹掉。
 * 顺带也就把拦截器、JSON 序列化、异常处理一起验了。
 */
public class AcceptanceApi {

    /** data.sql 里 5 个测试账号的统一口令 */
    public static final String PASSWORD = "123456";

    private final String base;
    private final ObjectMapper json;
    private final HttpClient http;

    private AcceptanceApi(String base, ObjectMapper json) {
        this.base = base;
        this.json = json;
        // CookieManager 自己存 JSESSIONID，不用手工搬 cookie
        this.http = HttpClient.newBuilder().cookieHandler(new CookieManager()).build();
    }

    /** 一个实例一个会话。换角色就再开一个，cookie 不会串。 */
    public static AcceptanceApi login(int port, ObjectMapper json, String username) {
        AcceptanceApi api = new AcceptanceApi("http://localhost:" + port + "/api", json);
        Resp resp = api.post("/auth/login", Map.of("username", username, "password", PASSWORD));
        if (!resp.ok()) {
            throw new IllegalStateException("以 " + username + " 登录失败：" + resp.text());
        }
        return api;
    }

    public Resp get(String path) {
        return send(HttpRequest.newBuilder(uri(path)).GET().build());
    }

    public Resp post(String path, Object body) {
        return send(HttpRequest.newBuilder(uri(path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(encode(body), StandardCharsets.UTF_8))
                .build());
    }

    public Resp put(String path, Object body) {
        return send(HttpRequest.newBuilder(uri(path))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(encode(body), StandardCharsets.UTF_8))
                .build());
    }

    public Resp delete(String path) {
        return send(HttpRequest.newBuilder(uri(path)).DELETE().build());
    }

    private Resp send(HttpRequest request) {
        try {
            HttpResponse<String> resp = http.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return new Resp(resp.statusCode(), json.readTree(resp.body()));
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException("请求失败：" + request.uri(), e);
        }
    }

    private URI uri(String path) {
        return URI.create(base + path);
    }

    private String encode(Object body) {
        try {
            return json.writeValueAsString(body);
        } catch (Exception e) {
            throw new IllegalStateException("请求体序列化失败", e);
        }
    }

    /** 取数组里的 id 字段。 */
    public static List<Long> ids(JsonNode array, String field) {
        List<Long> result = new ArrayList<>();
        for (JsonNode node : array) {
            result.add(node.path(field).asLong());
        }
        return result;
    }

    /**
     * 一次响应。**HTTP 状态码基本没用** —— GlobalExceptionHandler 恒返回 200，
     * 业务码在 body.code 里，前端也只看它。所以断言一律断 code() 和 msg()。
     */
    public record Resp(int status, JsonNode body) {

        public boolean ok() {
            return code() == 0;
        }

        public int code() {
            return body.path("code").asInt();
        }

        public String msg() {
            return body.path("msg").asText();
        }

        public JsonNode data() {
            return body.path("data");
        }

        public String text() {
            return body.toString();
        }

        /** 业务码必须是 0，否则直接把后端的报错信息抛出来，省得在断言里猜。 */
        public JsonNode require() {
            if (!ok()) {
                throw new AssertionError("接口返回失败：code=" + code() + " msg=" + msg());
            }
            return data();
        }
    }
}
