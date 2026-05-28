package com.moyuan.common;

import lombok.Data;

@Data
public class R<T> {
    private int code;
    private String message;
    private T data;

    public static <T> R<T> success(T data) {
        R<T> r = new R<>();
        r.setCode(200);
        r.setMessage("success");
        r.setData(data);
        return r;
    }

    public static <T> R<T> success() {
        return success(null);
    }

    public static <T> R<T> error(int code, String message) {
        R<T> r = new R<>();
        r.setCode(code);
        r.setMessage(message);
        return r;
    }

    public static <T> R<T> error(String message) {
        return error(500, message);
    }
}
