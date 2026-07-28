package com.bizzan.bitrade.entity;

import jakarta.persistence.Embeddable;
import lombok.Data;
import java.io.Serializable;

/**
 * 微信信息
 *
 * @author GS
 * @date 2018年01月16日
 */
@Data
@Embeddable
public class WechatPay implements Serializable {
    private static final long serialVersionUID = 1511509989072675896L;
    /**
     * 微信号
     */
    private String wechat;


    /**
     * 微信收款二维码
     */
    private String qrWeCodeUrl;
}
