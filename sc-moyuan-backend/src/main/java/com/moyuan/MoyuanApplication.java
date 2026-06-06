package com.moyuan;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.moyuan.mapper")
@EnableScheduling
public class MoyuanApplication {

    public static void main(String[] args) {
        SpringApplication.run(MoyuanApplication.class, args);
    }
}