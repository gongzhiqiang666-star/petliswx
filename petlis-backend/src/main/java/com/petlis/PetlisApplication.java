package com.petlis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * PETLIS后端应用主启动类
 *
 * 宠物实验室信息系统后端API服务
 * 提供微信小程序接口,支持用户认证、宠物管理、检测申请、报告查询等功能
 *
 * @author PETLIS Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableCaching
@org.springframework.scheduling.annotation.EnableScheduling
public class PetlisApplication {

    public static void main(String[] args) {
        SpringApplication.run(PetlisApplication.class, args);
        System.out.println("\n========================================");
        System.out.println("PETLIS Backend Started Successfully!");
        System.out.println("API Documentation: http://localhost:8080/api/swagger-ui.html");
        System.out.println("========================================\n");
    }
}
