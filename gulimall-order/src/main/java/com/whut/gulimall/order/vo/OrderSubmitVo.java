package com.whut.gulimall.order.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 封装订单提交的数据
 */
@Data
public class OrderSubmitVo {

    private Long addrId; // 收获地址id

    private Integer payType; // 支付类型

    // TODO 优惠、发票

    private String orderToken; // 防重令牌

    private BigDecimal payPrice; // 验价

    private String note; // 备注
}
