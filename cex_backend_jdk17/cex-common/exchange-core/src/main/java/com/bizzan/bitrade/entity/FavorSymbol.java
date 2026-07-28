package com.bizzan.bitrade.entity;

import lombok.Data;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Data
@Table(name="exchange_favor_symbol")
public class FavorSymbol {
    @Id
    @GeneratedValue
    private Long id;
    private String symbol;
    private Long memberId;
    private String addTime;
}
