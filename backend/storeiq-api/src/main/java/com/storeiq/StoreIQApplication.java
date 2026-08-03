package com.storeiq;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * StoreIQ — 智能数码零售管理系统
 *
 * @author storeiq
 */
@SpringBootApplication
public class StoreIQApplication {

    public static void main(String[] args) {
        SpringApplication.run(StoreIQApplication.class, args);
        System.out.println("===========================================");
        System.out.println("  StoreIQ API 启动成功!");
        System.out.println("  http://localhost:8002");
        System.out.println("===========================================");
    }
}
