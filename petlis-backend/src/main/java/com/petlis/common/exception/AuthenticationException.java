package com.petlis.common.exception;

/**
 * 认证异常
 *
 * 当用户认证失败时抛出此异常
 *
 * @author PETLIS Team
 */
public class AuthenticationException extends BusinessException {

    public AuthenticationException(String message) {
        super(401, message);
    }

    public AuthenticationException(String message, Throwable cause) {
        super(401, message, cause);
    }
}
