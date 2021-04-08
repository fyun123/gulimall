package com.whut.gulimall.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.whut.common.to.SeckillOrderTo;
import com.whut.common.utils.PageUtils;
import com.whut.gulimall.order.entity.OmsOrderEntity;
import com.whut.gulimall.order.vo.*;

import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * 订单
 *
 * @author fangyun
 * @email fangyun@gmail.com
 * @date 2021-01-23 20:54:49
 */
public interface OmsOrderService extends IService<OmsOrderEntity> {

    PageUtils queryPage(Map<String, Object> params);

    /**
     * 确认订单
     * @return 订单信息（购物项、收获地址、应付金额）
     * @throws ExecutionException
     * @throws InterruptedException
     */
    OrderConfirmVo confirmOrder() throws ExecutionException, InterruptedException;

    /**
     * 提交订单
     * @param vo 页面提交信息
     * @return 订单、状态码、库存不足的skuId
     */
    SubmitOrderResponseVo submitOrder(OrderSubmitVo vo);

    /**
     * 获取当前订单的支付信息
     * @param orderSn 待支付订单的订单号
     * @return 支付基本信息
     */
    PayVo getOrderPay(String orderSn);

    /**
     * 分页查询订单及订单项
     * @param params 分页参数
     * @return 订单及订单项
     */
    PageUtils queryPageWithItems(Map<String, Object> params);

    /**
     * 处理支付宝返回数据
     * @param vo 支付宝异步通知返回数据
     * @return 返回支付状态
     */
    String handlePayResult(PayAsyncVo vo);

    /**
     * 通过订单号获取订单
     * @param orderSn 订单号
     * @return 订单
     */
    OmsOrderEntity getOrderByOrderSn(String orderSn);

    /**
     * 付款超时，关闭订单
     * @param orderEntity 订单
     */
    void closeOrder(OmsOrderEntity orderEntity);

    /**
     * 创建秒杀订单
     * @param seckillOrder 秒杀信息（商品，会员，秒杀价）
     */
    void createSeckillOrder(SeckillOrderTo seckillOrder);

    OrderConfirmVo toSeckillConfirm(String orderSn) throws ExecutionException, InterruptedException;

    SubmitOrderResponseVo submitSeckillOrder(String orderSn, OrderSubmitVo vo);

}

