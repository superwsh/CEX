package com.bizzan.bitrade.entity;

import jakarta.persistence.Embeddable;
import lombok.Data;

import java.io.Serializable;

/**
 * 地址
 *
 * @author GS
 * @date 2018年01月02日
 */
@Data
@Embeddable
public class Location implements Serializable {
    private String country;
    private String province;
    private String city;
    private String district;
}
