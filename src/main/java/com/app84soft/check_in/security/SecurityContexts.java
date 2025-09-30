package com.app84soft.check_in.security;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SecurityContexts {

    private static final ThreadLocal<SecurityContexts> context = new ThreadLocal<>();

    private Object data;

    public static SecurityContexts getContext() {
        return context.get();
    }

    public static void newContext(Object data) {
        SecurityContexts contexts = new SecurityContexts();
        SecurityContexts.context.set(contexts);
        SecurityContexts.context.get().setData(data);
    }

    /** Xoá sạch context hiện tại (dùng ở filter finally) */
    public static void clear() {
        context.remove();
    }
}
