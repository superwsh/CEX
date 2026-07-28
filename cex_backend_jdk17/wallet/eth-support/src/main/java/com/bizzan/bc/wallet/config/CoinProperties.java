package com.bizzan.bc.wallet.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.BigInteger;

@Data
@Component
@ConfigurationProperties(prefix = "coin")
public class CoinProperties {

    private String keystorePath;
    private String withdrawWallet;
    private String withdrawWalletPassword;
    private String rpc;
    private String unit = "ETH";
    private String name = "Ethereum";
    private BigDecimal gasSpeedUp = BigDecimal.ONE;
    private BigInteger gasLimit = BigInteger.valueOf(21000);

}
