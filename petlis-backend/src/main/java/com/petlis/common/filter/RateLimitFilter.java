package com.petlis.common.filter;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 请求频率限制过滤器
 * 防止API滥用,限制同一IP的请求频率
 *
 * 规则:
 * - 同一IP每分钟最多60次请求
 * - 超过限制返回429 Too Many Requests
 *
 * @author PETLIS
 * @since 1.0.0
 */
@Component
@WebFilter(urlPatterns = "/*", filterName = "rateLimitFilter")
@Order(2) // 在XSS过滤器之后执行
public class RateLimitFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    /**
     * 限制数量:每分钟120次请求（开发环境）
     * 生产环境建议保持60次/分钟
     */
    private static final int RATE_LIMIT = 120;

    /**
     * 时间窗口:60秒
     */
    private static final long TIME_WINDOW = 60 * 1000L;

    /**
     * 请求计数器:记录每个IP的请求次数和时间戳
     * Key: IP地址
     * Value: [请求次数, 最后一次请求时间戳]
     */
    private final ConcurrentHashMap<String, RateLimitInfo> rateLimiter = new ConcurrentHashMap<>();

    /**
     * 初始化过滤器
     *
     * @param filterConfig 过滤器配置
     * @throws ServletException 初始化异常
     */
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("请求频率限制过滤器初始化成功 - 限制: {}次/分钟", RATE_LIMIT);

        // 启动清理过期数据的定时任务
        startCleanupTask();
    }

    /**
     * 执行过滤
     * 检查请求频率是否超过限制
     *
     * @param request  请求对象
     * @param response 响应对象
     * @param chain    过滤器链
     * @throws IOException      IO异常
     * @throws ServletException Servlet异常
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // 获取客户端IP
        String clientIp = getClientIp(httpRequest);

        // 检查请求频率
        if (isRateLimited(clientIp)) {
            log.warn("请求频率超限 - IP: {}, 限制: {}次/分钟", clientIp, RATE_LIMIT);

            // 返回429错误
            httpResponse.setStatus(429); // HTTP 429 Too Many Requests
            httpResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
            httpResponse.setCharacterEncoding(StandardCharsets.UTF_8.name());

            String responseBody = "{\"code\":429,\"message\":\"请求过于频繁,请稍后再试\"}";
            httpResponse.getWriter().write(responseBody);
            return;
        }

        // 继续执行过滤器链
        chain.doFilter(request, response);
    }

    /**
     * 检查是否超过请求频率限制
     *
     * @param clientIp 客户端IP
     * @return true=超过限制, false=未超过限制
     */
    private boolean isRateLimited(String clientIp) {
        long currentTime = System.currentTimeMillis();

        // 获取或创建该IP的限流信息
        RateLimitInfo info = rateLimiter.computeIfAbsent(clientIp, k -> new RateLimitInfo(currentTime));

        // 检查是否需要重置计数器(超过时间窗口)
        if (currentTime - info.lastRequestTime > TIME_WINDOW) {
            info.count.set(0);
            info.lastRequestTime = currentTime;
        }

        // 增加计数
        int currentCount = info.count.incrementAndGet();
        info.lastRequestTime = currentTime;

        // 检查是否超过限制
        return currentCount > RATE_LIMIT;
    }

    /**
     * 获取客户端真实IP
     * 支持代理和负载均衡
     *
     * @param request HTTP请求
     * @return 客户端IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // 如果通过多个代理,X-Forwarded-For会包含多个IP,取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip;
    }

    /**
     * 启动清理过期数据的定时任务
     * 每5分钟清理一次超过时间窗口的数据
     */
    private void startCleanupTask() {
        Thread cleanupThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(5 * 60 * 1000L); // 5分钟

                    long currentTime = System.currentTimeMillis();
                    int removedCount = 0;

                    // 清理过期数据
                    for (String key : rateLimiter.keySet()) {
                        RateLimitInfo info = rateLimiter.get(key);
                        if (info != null && currentTime - info.lastRequestTime > TIME_WINDOW) {
                            rateLimiter.remove(key);
                            removedCount++;
                        }
                    }

                    if (removedCount > 0) {
                        log.debug("清理过期限流数据: {} 条", removedCount);
                    }

                } catch (InterruptedException e) {
                    log.error("清理任务被中断", e);
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });

        cleanupThread.setDaemon(true);
        cleanupThread.setName("RateLimitCleanupThread");
        cleanupThread.start();
    }

    /**
     * 销毁过滤器
     */
    @Override
    public void destroy() {
        log.info("请求频率限制过滤器已销毁");
        rateLimiter.clear();
    }

    /**
     * 限流信息内部类
     */
    private static class RateLimitInfo {
        /**
         * 请求计数
         */
        AtomicInteger count;

        /**
         * 最后一次请求时间戳
         */
        long lastRequestTime;

        /**
         * 构造函数
         *
         * @param lastRequestTime 最后请求时间
         */
        RateLimitInfo(long lastRequestTime) {
            this.count = new AtomicInteger(0);
            this.lastRequestTime = lastRequestTime;
        }
    }
}
