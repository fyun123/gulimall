package com.whut.gulimall.order.vo;

import com.whut.gulimall.order.entity.OmsOrderEntity;
import io.netty.util.concurrent.OrderedEventExecutor;
import lombok.Data;

import java.util.List;

@Data
public class SubmitOrderResponseVo {

    private OmsOrderEntity order;

    private Integer code; // 错误状态码，0-成功，1-失败

    private List<Long> failSkuIds;
}
