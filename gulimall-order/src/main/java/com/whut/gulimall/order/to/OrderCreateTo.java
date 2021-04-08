package com.whut.gulimall.order.to;

import com.whut.gulimall.order.entity.OmsOrderEntity;
import com.whut.gulimall.order.entity.OmsOrderItemEntity;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
@Data
public class OrderCreateTo {

    private OmsOrderEntity order;

    private List<OmsOrderItemEntity> orderItems;

    private BigDecimal payPrice;

    private BigDecimal fare;

}
