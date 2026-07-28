package com.bizzan.bc.wallet.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "etherscan")
public class EtherscanProperties {
    private String apiKey;
}
