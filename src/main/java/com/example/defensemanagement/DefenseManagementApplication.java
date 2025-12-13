package com.example.defensemanagement;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.example.defensemanagement.mapper")
public class DefenseManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(DefenseManagementApplication.class, args);
    }

}
