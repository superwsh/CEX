package com.bizzan.bc.wallet.config;

import com.bizzan.bc.wallet.entity.Coin;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

import java.net.MalformedURLException;
import java.time.Duration;

@Configuration
//@EnableConfigurationProperties({CoinProperties.class, EtherscanProperties.class})
public class EthConfig {

    @Autowired
    private CoinProperties coin;

    @Bean
//    @ConditionalOnProperty(name = "coin.keystore-path")
    public Web3j web3j() {
        var httpClient = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(30))
                .writeTimeout(Duration.ofSeconds(30))
                .readTimeout(Duration.ofSeconds(30))
                .build();
        return Web3j.build(new HttpService(coin.getRpc(), httpClient, false));
    }

}
