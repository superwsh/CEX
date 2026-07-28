package com.bizzan.bc.wallet.entity;

import lombok.Builder;
import lombok.Data;
import org.web3j.crypto.Credentials;

import java.math.BigDecimal;
import java.math.BigInteger;

@Data
@Builder
public class Payment {
    private String txBizNumber;
    private String txid;
    private Credentials credentials;
    private String to;
    private BigDecimal amount;
    private String unit;
    private BigInteger gasLimit;
    private BigInteger gasPrice;
}
