package com.petlis.common.filter;

import com.petlis.common.security.UserPrincipal;
import com.petlis.common.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT认证过滤器
 *
 * 从请求头中提取JWT token并验证,设置认证信息到SecurityContext
 *
 * @author PETLIS Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final com.petlis.service.impl.SessionService sessionService;

    private static final String HEADER_NAME = "Authorization";
    private static final String TOKEN_PREFIX = "Bearer ";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String requestUri = request.getRequestURI();
        String requestMethod = request.getMethod();

        try {
            // 从请求头中获取token
            String token = extractToken(request);

            if (StringUtils.hasText(token) && sessionService.valid(token)) {
                // 解析token获取用户信息
                Integer userId = jwtUtil.getUserIdFromToken(token);
                String openid = jwtUtil.getOpenidFromToken(token);
                Integer hospitalId = jwtUtil.getHospitalIdFromToken(token);
                Integer cityId = jwtUtil.getCityIdFromToken(token);

                // 创建认证对象,将hospitalId、cityId和openid也存储在principal中
                UserPrincipal userPrincipal = new UserPrincipal(userId, openid, hospitalId, cityId);
                java.util.Set<String> authorities = new java.util.HashSet<>();
                var roles = sessionService.roles(userId);
                boolean reviewer = jwtUtil.isReviewerToken(token);
                if (reviewer) authorities.add("ROLE_REVIEWER");
                if (roles.contains(com.petlis.common.security.PlatformRole.DOCTOR)
                        || roles.isEmpty() && !reviewer && !jwtUtil.isStaffToken(token)) {
                    authorities.add("ROLE_USER");
                }
                for (var role : roles) authorities.add("ROLE_" + role.name());
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userPrincipal,
                                null,
                                authorities.stream().map(SimpleGrantedAuthority::new).toList()
                        );

                // 设置详情
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 设置到SecurityContext
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("用户认证成功 - userId: {}, hospitalId: {}, cityId: {}, URI: {} {}", userId, hospitalId, cityId, requestMethod, requestUri);
            } else {
                log.trace("未提供token或token无效, URI: {} {}", requestMethod, requestUri);
            }
        } catch (Exception e) {
            log.error("JWT认证失败 - URI: {} {}, 错误: {}", requestMethod, requestUri, e.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 从请求头中提取token
     *
     * @param request HTTP请求
     * @return JWT token,不包含Bearer前缀
     */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(HEADER_NAME);

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(TOKEN_PREFIX)) {
            return bearerToken.substring(TOKEN_PREFIX.length());
        }

        return null;
    }
}
