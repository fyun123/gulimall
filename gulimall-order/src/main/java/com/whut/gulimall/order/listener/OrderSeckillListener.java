package com.whut.gulimall.order.listener;

import com.rabbitmq.client.Channel;
import com.whut.common.to.SeckillOrderTo;
import com.whut.gulimall.order.entity.OmsOrderEntity;
import com.whut.gulimall.order.service.OmsOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;

@Slf4j
@Component
@RabbitListener(queues = "order.seckill.order.queue")
public class OrderSeckillListener {

    @Autowired
    OmsOrderService orderService;

    @RabbitHandler
    public void listener(SeckillOrderTo seckillOrder, Channel channel, Message message) throws IOException {

        try{
            log.info("准备创建秒杀单信息");
            orderService.createSeckillOrder(seckillOrder);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        }catch (Exception e){
            log.info("创建失败");
            System.out.println(Arrays.toString(e.getStackTrace()));
            channel.basicReject(message.getMessageProperties().getDeliveryTag(), true);

        }
    }
}
