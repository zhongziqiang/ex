package com.kline.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaTopicConfig {
    @Bean
    public NewTopic myTopic() {
        return new NewTopic("my-topic", 1, (short) 1); // 主题名称，分区数，副本数
    }
}
