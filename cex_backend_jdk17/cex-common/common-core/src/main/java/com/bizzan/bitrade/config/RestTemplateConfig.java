package com.bizzan.bitrade.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestTemplateConfig {
    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        // 设置连接超时（5秒）
        factory.setConnectTimeout((int) Duration.ofSeconds(5).toMillis());
        // 设置读取超时（10秒）
        factory.setReadTimeout((int) Duration.ofSeconds(10).toMillis());
        // 创建RestTemplate并绑定工厂
        RestTemplate restTemplate = new RestTemplate(factory);
        // 配置消息转换器（默认已支持UTF-8，无需额外配置）
        return restTemplate;
    }
}
