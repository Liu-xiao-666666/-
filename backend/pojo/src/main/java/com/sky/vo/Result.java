package com.sky.vo;

import lombok.Data;

@Data
public class Result<T> {
    private Integer code;
    private String message;
    private T data;

    public static <T> Result<T> success(T data) {
        Result<T> r = new Result<>();
        r.code = 1;
        r.message = "success";
        r.data = data;
        return r;
    }

    public static Result<Void> success() {
        Result<Void> r = new Result<>();
        r.code = 1;
        r.message = "success";
        return r;
    }

    public static Result<Void> error(String message) {
        Result<Void> r = new Result<>();
        r.code = 0;
        r.message = message;
        return r;
    }
}
