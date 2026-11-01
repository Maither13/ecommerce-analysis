package com.example.ecommerce;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.example.ecommerce.**.mapper")
public class EcommerceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EcommerceApplication.class, args);
        System.out.println("========================================");
        System.out.println("  电商平台用户行为分析系统启动成功!");
        System.out.println("  API文档: http://localhost:8080/api/swagger-ui.html");
        System.out.println("========================================");
    }
}
