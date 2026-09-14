package com.kelifang.common;

/**
 * 当前登录用户的线程上下文。由 LoginInterceptor 写入和清理，Service 层直接取。
 */
public final class UserContext {

    public static final String SESSION_KEY = "currentUser";

    private static final ThreadLocal<CurrentUser> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(CurrentUser user) {
        HOLDER.set(user);
    }

    /** 未登录时返回 null。方法内部按角色过滤数据时用。 */
    public static CurrentUser get() {
        return HOLDER.get();
    }

    public static Long userId() {
        CurrentUser user = HOLDER.get();
        return user == null ? null : user.getId();
    }

    public static String role() {
        CurrentUser user = HOLDER.get();
        return user == null ? null : user.getRole();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
