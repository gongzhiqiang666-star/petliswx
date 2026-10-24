package com.petlis.common.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 性能监控切面
 * 记录方法执行时间,超过300ms的记录WARN日志
 *
 * @author PETLIS
 * @since 1.0.0
 */
@Aspect
@Component
public class PerformanceLogAspect {

    private static final Logger log = LoggerFactory.getLogger(PerformanceLogAspect.class);

    /**
     * 慢查询阈值:300ms
     */
    private static final long SLOW_QUERY_THRESHOLD = 300L;

    /**
     * 定义切点:拦截所有Controller、Service、Repository的方法
     */
    @Pointcut("execution(* com.petlis.controller..*.*(..)) || " +
              "execution(* com.petlis.service..*.*(..)) || " +
              "execution(* com.petlis.repository..*.*(..))")
    public void performancePointcut() {
        // 切点定义
    }

    /**
     * 环绕通知:记录方法执行时间
     *
     * @param joinPoint 连接点
     * @return 方法执行结果
     * @throws Throwable 执行异常
     */
    @Around("performancePointcut()")
    public Object logPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        // 执行方法
        Object result = joinPoint.proceed();

        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;

        // 获取方法信息
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        // 记录执行时间
        if (executionTime > SLOW_QUERY_THRESHOLD) {
            // 慢查询记录WARN日志
            log.warn("【性能警告】方法执行过慢 - {}.{} 执行时间: {}ms",
                    className, methodName, executionTime);
        } else if (log.isDebugEnabled()) {
            // 正常查询记录DEBUG日志
            log.debug("【性能监控】{}.{} 执行时间: {}ms",
                    className, methodName, executionTime);
        }

        return result;
    }
}
