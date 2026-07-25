package com.smartexpense;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.smartexpense.mapper")
@EnableScheduling
public class SmartExpenseApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartExpenseApplication.class, args);
        System.out.println("========================================");
        System.out.println("  智能差旅报销系统启动成功!");
        System.out.println("  Smart Expense Reimbursement System");
        System.out.println("  API Docs: http://localhost:8080/swagger-ui/index.html");
        System.out.println("========================================");
    }
}
