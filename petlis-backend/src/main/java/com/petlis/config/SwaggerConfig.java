package com.petlis.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Swagger配置
 *
 * 配置API文档生成
 *
 * @author PETLIS Team
 */
@Configuration
public class SwaggerConfig {

    /**
     * OpenAPI配置
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("PETLIS Backend API")
                        .description("宠物实验室信息系统后端API服务文档")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("PETLIS Team")
                                .email("support@petlis.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .servers(List.of(
                        new Server().url("http://localhost:8080/api").description("开发环境"),
                        new Server().url("https://api.petlis.com/api").description("生产环境")
                ))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("Bearer Authentication",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("请输入JWT token,格式: Bearer {token}")));
    }

    /**
     * API分组配置 - 认证接口
     */
    @Bean
    public GroupedOpenApi authApi() {
        return GroupedOpenApi.builder()
                .group("01-认证管理")
                .pathsToMatch("/auth/**")
                .build();
    }

    /**
     * API分组配置 - 宠物管理
     */
    @Bean
    public GroupedOpenApi petApi() {
        return GroupedOpenApi.builder()
                .group("02-宠物管理")
                .pathsToMatch("/pets/**")
                .build();
    }

    /**
     * API分组配置 - 订单管理
     */
    @Bean
    public GroupedOpenApi orderApi() {
        return GroupedOpenApi.builder()
                .group("03-订单管理")
                .pathsToMatch("/orders/**")
                .build();
    }

    /**
     * API分组配置 - 报告查询
     */
    @Bean
    public GroupedOpenApi reportApi() {
        return GroupedOpenApi.builder()
                .group("04-报告查询")
                .pathsToMatch("/reports/**")
                .build();
    }

    /**
     * API分组配置 - 字典管理
     */
    @Bean
    public GroupedOpenApi dictionaryApi() {
        return GroupedOpenApi.builder()
                .group("05-字典管理")
                .pathsToMatch("/dictionary/**")
                .build();
    }

    /**
     * API分组配置 - 用户管理
     */
    @Bean
    public GroupedOpenApi userApi() {
        return GroupedOpenApi.builder()
                .group("06-用户管理")
                .pathsToMatch("/user/**")
                .build();
    }
}
