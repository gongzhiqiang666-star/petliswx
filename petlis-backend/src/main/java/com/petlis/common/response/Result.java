package com.petlis.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;

/**
 * 统一响应对象
 *
 * 所有API接口统一返回此格式
 *
 * @param <T> 数据类型
 * @author PETLIS Team
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 响应码
     */
    private Integer code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    public static <T> Result<T> fromException(Exception error) {
        if (error instanceof com.petlis.common.exception.BusinessException business) return Result.fail(business.getCode(), business.getMessage());
        return Result.fail(500, "操作失败，请稍后重试");
    }

    public Result() {
    }

    public Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * 成功响应
     */
    public static <T> Result<T> success() {
        return new Result<>(200, "操作成功", null);
    }

    /**
     * 成功响应(带数据)
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "操作成功", data);
    }

    /**
     * 成功响应(自定义消息)
     */
    public static <T> Result<T> success(String message) {
        return new Result<>(200, message, null);
    }

    /**
     * 成功响应(自定义消息和数据)
     */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(200, message, data);
    }

    /**
     * 失败响应
     */
    public static <T> Result<T> fail(String message) {
        return new Result<>(500, message, null);
    }

    /**
     * 失败响应(自定义错误码)
     */
    public static <T> Result<T> fail(Integer code, String message) {
        return new Result<>(code, message, null);
    }

    /**
     * 失败响应(使用ResultCode)
     */
    public static <T> Result<T> fail(ResultCode resultCode) {
        return new Result<>(resultCode.getCode(), resultCode.getMessage(), null);
    }

    /**
     * 错误响应(error方法的别名)
     */
    public static <T> Result<T> error(String message) {
        return fail(message);
    }

    /**
     * 错误响应(自定义错误码)
     */
    public static <T> Result<T> error(Integer code, String message) {
        return fail(code, message);
    }
}
