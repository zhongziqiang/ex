package com.org.kline;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@MapperScan("com.org.kline.mapper")
public class KlineApplication {

	public static void main(String[] args) {
		SpringApplication.run(KlineApplication.class, args);
	}

}
