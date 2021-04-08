package com.whut.gulimall.order.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

//订单确认页需要的数据
@ToString
public class OrderConfirmVo {

    // 收获地址
    @Setter @Getter
    private List<MemberAddressVo> address;

    // 所有选中的购物项
    @Setter @Getter
    private List<OrderItemVo> items;

    // 发票信息

    // 积分信息
    @Setter @Getter
    private Integer integration;

    // 订单防重令牌
    @Setter @Getter
    private String orderToken;

    @Setter @Getter
    private Map<Long, Boolean> stocks;
    //订单总额
//    private BigDecimal total;

    public Integer getCount(){
        Integer i = 0;
        if (items != null){
            for (OrderItemVo item : items) {
                i += item.getCount();
            }
        }
        return i;
    }

    public BigDecimal getTotal() {
        BigDecimal sum = new BigDecimal("0.00");
        if (items != null){
            for (OrderItemVo item : items) {
                BigDecimal multiply = item.getPrice().multiply(new BigDecimal(item.getCount().toString()));
                sum = sum.add(multiply);
            }
        }

        return sum;
    }

    // 应付价格
//    private BigDecimal payPrice;

    public BigDecimal getPayPrice() {
        return getTotal();
    }
}
