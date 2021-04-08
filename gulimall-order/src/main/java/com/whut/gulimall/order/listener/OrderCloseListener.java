package com.whut.gulimall.order.listener;

import com.rabbitmq.client.Channel;
import com.whut.gulimall.order.entity.OmsOrderEntity;
import com.whut.gulimall.order.service.OmsOrderService;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

@RabbitListener(queues = "order.release.order.queue")
@Service
public class OrderCloseListener {

    @Autowired
    OmsOrderService orderService;

    @RabbitHandler
    public void listener(OmsOrderEntity orderEntity, Channel channel, Message message) throws IOException {
        System.out.println("收到过期订单，准备关单："+orderEntity.getOrderSn());
        try{
            orderService.closeOrder(orderEntity);
            // 手动调用支付宝收单

            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        }catch (Exception e){
            channel.basicReject(message.getMessageProperties().getDeliveryTag(), true);

        }
    }
}
