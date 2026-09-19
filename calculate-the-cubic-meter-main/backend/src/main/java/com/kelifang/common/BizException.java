package com.kelifang.common;

import lombok.Getter;

/**
 * 业务异常。Service 层遇到业务规则不满足时抛出，由 GlobalExceptionHandler 统一转成 Result。
 */
@Getter
public class BizException extends RuntimeException {

    private final int code;

    public BizException(String msg) {
        this(500, msg);
    }

    public BizException(int code, String msg) {
        super(msg);
        this.code = code;
    }

    public static BizException badRequest(String msg) {
        return new BizException(400, msg);
    }

    public static BizException notFound(String msg) {
        return new BizException(404, msg);
    }

    public static BizException forbidden(String msg) {
        return new BizException(403, msg);
    }
}
