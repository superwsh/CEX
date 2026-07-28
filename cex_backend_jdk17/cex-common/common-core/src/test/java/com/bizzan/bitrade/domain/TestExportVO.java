package com.bizzan.bitrade.domain;

import cn.afterturn.easypoi.excel.annotation.Excel;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class TestExportVO {
    @Excel(name = "订单编号", width = 20) // Excel列名+列宽
    private String orderNo;

    @Excel(name = "交易金额", width = 15, numFormat = "0.00") // 数字格式
    private BigDecimal amount;

    @Excel(name = "交易时间", width = 25, format = "yyyy-MM-dd HH:mm:ss") // 日期格式
    private Date createTime;

    @Excel(name = "用户名称", width = 15)
    private String userName;
}
