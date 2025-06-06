package com.org.kline.interceptor;

import org.apache.ibatis.plugin.Interceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MyBatisConfig {
    @Bean
    public Interceptor getPageInterceptor(){
        return new PageInterceptor();
    }
}
