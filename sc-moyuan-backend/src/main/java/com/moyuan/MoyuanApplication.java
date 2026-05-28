package com.moyuan;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 古今诗话——墨渊 后端启动类
 */
@SpringBootApplication
@MapperScan("com.moyuan.mapper")
public class MoyuanApplication {

    public static void main(String[] args) {
        SpringApplication.run(MoyuanApplication.class, args);
    }
}