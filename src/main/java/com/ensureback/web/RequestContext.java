package com.ensureback.web;

public final class RequestContext {

    private static final ThreadLocal<String> CORRELATION_ID = new ThreadLocal<>();

    private RequestContext() {
    }

    public static void setCorrelationId(String id) {
        CORRELATION_ID.set(id);
    }

    public static String getCorrelationId() {
        return CORRELATION_ID.get();
    }

    public static void clear() {
        CORRELATION_ID.remove();
    }
}