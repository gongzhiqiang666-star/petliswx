package com.petlis.common.exception;

import com.petlis.common.enums.ResultCodeEnum;
import com.petlis.common.response.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 *
 * 统一处理所有异常,返回标准的响应格式
 *
 * @author PETLIS Team
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<?> handleBusinessException(BusinessException e) {
        log.error("Business exception: {}", e.getMessage(), e);
        return Result.fail(e.getCode(), e.getMessage());
    }

    /**
     * 资源未找到异常
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<?> handleResourceNotFoundException(ResourceNotFoundException e) {
        log.error("Resource not found: {}", e.getMessage(), e);
        return Result.fail(404, e.getMessage());
    }

    /**
     * 认证异常
     */
    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<?> handleAuthenticationException(AuthenticationException e) {
        log.error("Authentication failed: {}", e.getMessage(), e);
        return Result.fail(401, e.getMessage());
    }

    /**
     * 参数校验异常(@Valid)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.error("Validation failed: {}", e.getMessage());
        String errorMsg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return Result.fail(400, "参数校验失败: " + errorMsg);
    }

    /**
     * 参数绑定异常
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleBindException(BindException e) {
        log.error("Bind exception: {}", e.getMessage());
        String errorMsg = e.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return Result.fail(400, "参数绑定失败: " + errorMsg);
    }

    /**
     * 约束违反异常
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleConstraintViolationException(ConstraintViolationException e) {
        log.error("Constraint violation: {}", e.getMessage());
        String errorMsg = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));
        return Result.fail(400, "参数验证失败: " + errorMsg);
    }

    /**
     * 缺少请求参数异常
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        log.error("Missing parameter: {}", e.getMessage());
        return Result.fail(400, "缺少必需参数: " + e.getParameterName());
    }

    /**
     * 参数类型不匹配异常
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        log.error("Type mismatch: {}", e.getMessage());
        return Result.fail(400, "参数类型错误: " + e.getName());
    }

    /**
     * 请求方法不支持异常
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public Result<?> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        log.error("Method not supported: {}", e.getMessage());
        return Result.fail(405, "请求方法不支持: " + e.getMethod());
    }

    /**
     * 无权限异常
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<?> handleAccessDeniedException(AccessDeniedException e) {
        log.error("Access denied: {}", e.getMessage());
        return Result.fail(403, "无权限访问");
    }

    /**
     * 错误凭证异常
     */
    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<?> handleBadCredentialsException(BadCredentialsException e) {
        log.error("Bad credentials: {}", e.getMessage());
        return Result.fail(401, "用户名或密码错误");
    }

    /**
     * 404异常
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<?> handleNoHandlerFoundException(NoHandlerFoundException e) {
        log.error("No handler found: {}", e.getMessage());
        return Result.fail(404, "接口不存在: " + e.getRequestURL());
    }

    /**
     * 其他未捕获异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<?> handleException(Exception e) {
        log.error("System error: {}", e.getMessage(), e);
        return Result.fail(ResultCodeEnum.INTERNAL_SERVER_ERROR);
    }
}
