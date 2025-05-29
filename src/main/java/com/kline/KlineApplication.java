package com.kline;

import ch.qos.logback.classic.LoggerContext;
import org.mybatis.spring.annotation.MapperScan;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@MapperScan("com.kline.mapper")
public class KlineApplication {

    public static void main(String[] args) {
        SpringApplication.run(KlineApplication.class, args);
    }

}
