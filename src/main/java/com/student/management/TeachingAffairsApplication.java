package com.student.management;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.student.management.mapper")
public class TeachingAffairsApplication {
    public static void main(String[] args) {
        SpringApplication.run(TeachingAffairsApplication.class, args);
    }
}
