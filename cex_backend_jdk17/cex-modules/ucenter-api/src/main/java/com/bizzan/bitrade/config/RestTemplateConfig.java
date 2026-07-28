package com.bizzan.bitrade.config;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * @author GS
 * @date 2018年02月27日
 */
@Configuration
public class RestTemplateConfig {

    /*@Bean
    public ClientHttpRequestFactory simpleClientHttpRequestFactory(){
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setReadTimeout(5000);//单位为ms
        factory.setConnectTimeout(5000);//单位为ms
        return factory;
    }

    @LoadBalanced
    @Bean
    public RestTemplate restTemplate(ClientHttpRequestFactory factory){
        return new RestTemplate(factory);
    }*/

    @Bean
    @LoadBalanced
    RestTemplate restTemplate() {
        // 配置 HttpClient 连接池
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setDefaultMaxPerRoute(20);
        connectionManager.setMaxTotal(100);

        // 2.2 请求级超时配置（兜底）
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofMilliseconds(5000)) // 连接池获取连接超时 5s
                .setConnectTimeout(Timeout.ofMilliseconds(5000))           // 连接超时 5s
                .build();
        // 3. 创建带连接池的 CloseableHttpClient（核心：Spring 6.x 需通过客户端传入连接池）
        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager) // 绑定连接池
                .setDefaultRequestConfig(requestConfig)  // 绑定超时配置
                .build();
        // 配置超时
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
        return new RestTemplate(factory);
    }

}
