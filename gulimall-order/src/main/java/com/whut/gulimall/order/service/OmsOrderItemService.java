package com.whut.gulimall.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.whut.common.utils.PageUtils;
import com.whut.gulimall.order.entity.OmsOrderItemEntity;
import com.whut.gulimall.order.vo.OrderItemVo;

import java.util.Map;

/**
 * 订单项信息
 *
 * @author fangyun
 * @email fangyun@gmail.com
 * @date 2021-01-23 20:54:49
 */
public interface OmsOrderItemService extends IService<OmsOrderItemEntity> {

    PageUtils queryPage(Map<String, Object> params);

    OrderItemVo getItemByOrderSn(String orderSn);
}

