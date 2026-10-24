package com.petlis.common.aspect;

import com.petlis.common.annotation.OperationLog;
import com.petlis.common.security.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 操作日志切面
 *
 * 拦截带有@OperationLog注解的方法,记录操作日志
 *
 * @author PETLIS Team
 * @since 1.0.0
 */
@Slf4j
@Aspect
@Component
public class OperationLogAspect {

    /**
     * 环绕通知,记录操作日志
     */
    @Around("@annotation(com.petlis.common.annotation.OperationLog)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取方法签名
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        // 获取注解
        OperationLog operationLog = method.getAnnotation(OperationLog.class);
        if (operationLog == null) {
            return joinPoint.proceed();
        }

        // 获取操作信息
        String module = operationLog.module();
        String operation = operationLog.operation();
        String description = operationLog.description();

        // 获取当前用户信息
        Integer userId = null;
        Integer hospitalId = null;
        try {
            userId = SecurityUtils.getCurrentUserId();
            hospitalId = SecurityUtils.getCurrentHospitalId();
        } catch (Exception e) {
            log.trace("未登录用户操作");
        }

        // 记录开始时间
        long startTime = System.currentTimeMillis();

        // 执行方法
        Object result = null;
        try {
            result = joinPoint.proceed();

            // 记录成功日志
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            log.info("操作日志 - 模块: {}, 操作: {}, 描述: {}, 用户: {}, 医院: {}, 耗时: {}ms",
                    module, operation, description, userId, hospitalId, duration);

            return result;
        } catch (Exception e) {
            // 记录失败日志
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            log.error("操作日志 - 模块: {}, 操作: {}, 描述: {}, 用户: {}, 医院: {}, 失败: {}, 耗时: {}ms",
                    module, operation, description, userId, hospitalId, e.getMessage(), duration);

            throw e;
        }
    }
}
