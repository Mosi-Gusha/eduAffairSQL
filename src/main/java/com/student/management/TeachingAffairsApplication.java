package com.student.management;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.student.management.mapper")
@EnableScheduling
public class TeachingAffairsApplication {
    public static void main(String[] args) {
        SpringApplication.run(TeachingAffairsApplication.class, args);
    }
}
