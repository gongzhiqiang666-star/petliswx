package com.petlis.common.exception;

import lombok.Getter;

/**
 * 业务异常
 *
 * 用于业务逻辑中的异常情况,如参数校验失败、权限不足等
 *
 * @author PETLIS Team
 */
@Getter
public class BusinessException extends RuntimeException {

    private final Integer code;

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(String message) {
        this(500, message);
    }

    public BusinessException(Integer code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }
}
