package com.petlis.config;

import com.petlis.common.filter.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security配置
 *
 * 配置JWT认证
 *
 * @author PETLIS Team
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    /**
     * Security过滤器链配置
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 禁用CSRF
                .csrf(csrf -> csrf.disable())

                // 配置会话管理为无状态
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 配置授权规则
                .authorizeHttpRequests(auth -> auth
                        // 公开接口:认证相关
                        .requestMatchers("/auth/**").permitAll()
                        // 公开接口:字典数据(用于测试)
                        .requestMatchers("/dictionary/**").permitAll()
                        // 公开接口:Swagger UI
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                        // 公开接口:静态资源(上传的文件)
                        .requestMatchers("/uploads/**", "/files/**").permitAll()
                        // 公开接口:健康检查
                        .requestMatchers("/actuator/**").permitAll()
                        // 公开接口:错误处理
                        .requestMatchers("/error").permitAll()
                        // 其他所有接口需要认证
                        .anyRequest().authenticated()
                )

                // 添加JWT过滤器
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
