package com.whut.gulimall.order.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderFareVo {

    private MemberAddressVo address;

    private BigDecimal fare;
}
