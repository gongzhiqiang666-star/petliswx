package com.petlis.common.filter;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * XSS攻击防护过滤器
 * 拦截所有HTTP请求,对请求参数进行XSS过滤
 *
 * @author PETLIS
 * @since 1.0.0
 */
@Component
@WebFilter(urlPatterns = "/*", filterName = "xssFilter")
@Order(1) // 设置高优先级,确保在其他过滤器之前执行
public class XssFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(XssFilter.class);

    /**
     * 初始化过滤器
     *
     * @param filterConfig 过滤器配置
     * @throws ServletException 初始化异常
     */
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("XSS过滤器初始化成功");
    }

    /**
     * 执行过滤
     * 使用XssWrapper包装请求,对参数进行XSS过滤
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

        // 包装请求,进行XSS过滤
        XssWrapper xssRequest = new XssWrapper(httpRequest);

        // 记录XSS过滤(仅DEBUG模式)
        if (log.isDebugEnabled()) {
            String uri = httpRequest.getRequestURI();
            log.debug("XSS过滤器处理请求: {}", uri);
        }

        // 继续执行过滤器链
        chain.doFilter(xssRequest, response);
    }

    /**
     * 销毁过滤器
     */
    @Override
    public void destroy() {
        log.info("XSS过滤器已销毁");
    }
}
