package com.whut.gulimall.order.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
@Data
public class OrderItemVo {


    private Long skuId;

    private String title;

    private String Image;

    private List<String> skuAttr;

    private BigDecimal price;

    private Integer count;

    private BigDecimal totalPrice;

    // 重量
    private BigDecimal weight;
}
