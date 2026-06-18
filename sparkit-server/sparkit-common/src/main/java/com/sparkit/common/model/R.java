package com.sparkit.common.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 统一响应体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class R<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private int code;
    private String msg;
    private T data;
    private long timestamp;

    public static <T> R<T> ok() {
        return new R<>(200, "操作成功", null, System.currentTimeMillis());
    }

    public static <T> R<T> ok(T data) {
        return new R<>(200, "操作成功", data, System.currentTimeMillis());
    }

    public static <T> R<T> ok(String msg, T data) {
        return new R<>(200, msg, data, System.currentTimeMillis());
    }

    public static <T> R<T> fail(int code, String msg) {
        return new R<>(code, msg, null, System.currentTimeMillis());
    }

    public static <T> R<T> fail(String msg) {
        return new R<>(500, msg, null, System.currentTimeMillis());
    }

    public static <T> R<T> fail(int code, String msg, T data) {
        return new R<>(code, msg, data, System.currentTimeMillis());
    }
}