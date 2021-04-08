package com.whut.gulimall.order.service.impl;

import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.whut.common.utils.PageUtils;
import com.whut.common.utils.Query;

import com.whut.gulimall.order.dao.OmsRefundInfoDao;
import com.whut.gulimall.order.entity.OmsRefundInfoEntity;
import com.whut.gulimall.order.service.OmsRefundInfoService;

@RabbitListener(queues = "hello-java-queue")
@Service("omsRefundInfoService")
public class OmsRefundInfoServiceImpl extends ServiceImpl<OmsRefundInfoDao, OmsRefundInfoEntity> implements OmsRefundInfoService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OmsRefundInfoEntity> page = this.page(
                new Query<OmsRefundInfoEntity>().getPage(params),
                new QueryWrapper<OmsRefundInfoEntity>()
        );

        return new PageUtils(page);
    }

//    @RabbitListener(queues = "hello-java-queue")
    @RabbitHandler
    public void receive(Message message, OmsRefundInfoEntity book, Channel channel){
        System.out.println("收到消息："+message+"内容:"+book);
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            channel.basicAck(deliveryTag,false);
            channel.basicNack(deliveryTag,false,false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}