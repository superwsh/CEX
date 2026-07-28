package com.bizzan.bitrade.config;

import com.aqmd.netty.push.HawkPushServiceApi;
import com.aqmd.netty.push.impl.HawkPushServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NettyPushConfig {

    @Bean
    public HawkPushServiceApi hawkPushServiceApi() {
        return new HawkPushServiceImpl();
    }

}
