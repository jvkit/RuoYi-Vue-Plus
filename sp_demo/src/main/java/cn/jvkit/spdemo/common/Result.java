package cn.jvkit.spdemo.common;

import lombok.Data;

/**
 * 统一响应结果
 *
 * @param <T> 返回数据的类型
 */
@Data
public class Result<T> {

    /**
     * 状态码：200 成功，500 失败
     */
    private int code;

    /**
     * 提示信息
     */
    private String msg;

    /**
     * 返回数据
     */
    private T data;

    public static <T> Result<T> ok(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMsg("success");
        result.setData(data);
        return result;
    }

    public static <T> Result<T> ok() {
        return ok(null);
    }

    public static <T> Result<T> fail(String msg) {
        Result<T> result = new Result<>();
        result.setCode(500);
        result.setMsg(msg);
        return result;
    }

}
