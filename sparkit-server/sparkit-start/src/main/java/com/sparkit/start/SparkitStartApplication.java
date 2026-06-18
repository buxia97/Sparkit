package com.sparkit.start;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Sparkit 通用开发框架 - 启动入口
 */
@SpringBootApplication(scanBasePackages = "com.sparkit")
public class SparkitStartApplication {

    public static void main(String[] args) {
        SpringApplication.run(SparkitStartApplication.class, args);
        System.out.println("============================================");
        System.out.println("  Sparkit 启动成功!");
        System.out.println("============================================");
    }
}
