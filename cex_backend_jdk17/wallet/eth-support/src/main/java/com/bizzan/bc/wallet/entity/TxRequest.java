package com.bizzan.bc.wallet.entity;

import lombok.Builder;
import lombok.Getter;
import okhttp3.Credentials;
import java.math.BigDecimal;

@Builder
@Getter
public class TxRequest {
    private Credentials credentials;
    private String to;
    private BigDecimal amount;
    private String bizId;
    private boolean token;
}
