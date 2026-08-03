package com.storeiq.common;

/**
 * 统一响应结果封装
 *
 * @param <T> 数据类型
 */
public class Result<T> {

    private int code;
    private String message;
    private Long total;
    private T data;

    public Result() {}

    public Result(int code, String message) {
        this.code = code;
        this.message = message;
    }

    // ---------- 静态工厂方法 ----------

    public static <T> Result<T> success() {
        return new Result<>(200, "操作成功");
    }

    public static <T> Result<T> success(T data) {
        Result<T> r = new Result<>(200, "操作成功");
        r.setData(data);
        return r;
    }

    public static <T> Result<T> success(T data, Long total) {
        Result<T> r = success(data);
        r.setTotal(total);
        return r;
    }

    public static <T> Result<T> success(String message, T data) {
        Result<T> r = new Result<>(200, message);
        r.setData(data);
        return r;
    }

    public static <T> Result<T> fail() {
        return new Result<>(400, "操作失败");
    }

    public static <T> Result<T> fail(String message) {
        return new Result<>(400, message);
    }

    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message);
    }

    public static <T> Result<T> unauthorized(String message) {
        return new Result<>(401, message);
    }

    public static <T> Result<T> forbidden(String message) {
        return new Result<>(403, message);
    }

    // ---------- Getter / Setter ----------

    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Long getTotal() { return total; }
    public void setTotal(Long total) { this.total = total; }

    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
}
