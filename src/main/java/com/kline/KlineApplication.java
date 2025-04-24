package com.kline;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.kline.mapper")
public class KlineApplication {

    public static void main(String[] args) {
        SpringApplication.run(KlineApplication.class, args);
    }

}
