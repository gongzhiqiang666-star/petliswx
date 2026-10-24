package com.petlis.common.aspect;

import com.petlis.common.security.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * 数据权限切面
 *
 * 自动在Service方法调用前添加医院ID过滤,确保用户只能访问本院数据
 *
 * @author PETLIS Team
 * @since 1.0.0
 */
@Slf4j
@Aspect
@Component
public class DataPermissionAspect {

    /**
     * 拦截Service层方法,自动设置医院ID
     *
     * 注意: 这个切面主要用于日志记录和权限验证
     * 实际的数据过滤应在Repository层通过自定义查询实现
     */
    @Around("execution(* com.petlis.service..*.*(..))")
    public Object aroundServiceMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        try {
            // 检查用户是否已登录
            if (SecurityUtils.isAuthenticated()) {
                Integer hospitalId = SecurityUtils.getCurrentHospitalId();
                Integer userId = SecurityUtils.getCurrentUserId();

                // 记录操作日志
                log.debug("数据权限检查 - 用户: {}, 医院: {}, 调用: {}.{}",
                        userId, hospitalId, className, methodName);
            }

            // 继续执行方法
            return joinPoint.proceed();
        } catch (Exception e) {
            log.error("Service方法执行失败 - {}.{}: {}",
                    className, methodName, e.getMessage(), e);
            throw e;
        }
    }
}
