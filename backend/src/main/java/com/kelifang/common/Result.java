package com.kelifang.common;

import lombok.Data;

/**
 * 统一响应体。code 为 0 表示成功，非 0 为业务错误码（前端只看 code，不区分 HTTP 状态）。
 */
@Data
public class Result<T> {

    public static final int OK = 0;
    public static final int UNAUTHORIZED = 401;

    private int code;
    private String msg;
    private T data;

    private Result(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public static <T> Result<T> ok() {
        return new Result<>(OK, "ok", null);
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>(OK, "ok", data);
    }

    public static <T> Result<T> fail(String msg) {
        return new Result<>(500, msg, null);
    }

    public static <T> Result<T> fail(int code, String msg) {
        return new Result<>(code, msg, null);
    }
}
