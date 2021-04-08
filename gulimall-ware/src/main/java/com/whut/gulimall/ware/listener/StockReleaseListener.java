package com.whut.gulimall.ware.listener;

import com.alibaba.fastjson.TypeReference;
import com.rabbitmq.client.Channel;
import com.whut.common.to.OrderTo;
import com.whut.common.to.StockDetailTo;
import com.whut.common.to.StockLockedTo;
import com.whut.common.utils.R;
import com.whut.gulimall.ware.entity.WareOrderTaskDetailEntity;
import com.whut.gulimall.ware.entity.WareOrderTaskEntity;
import com.whut.gulimall.ware.service.WareSkuService;
import com.whut.gulimall.ware.vo.OrderVo;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@RabbitListener(queues = "stock.release.stock.queue")
public class StockReleaseListener {

    @Autowired
    WareSkuService wareSkuService;

    /**
     * 库存自动解锁
     *只要解锁库存的消息的失败，需要告诉MQ解锁失败，消息不要删除，重新放回队列
     * @param lockedTo
     * @param message
     *
     */
    @RabbitHandler
    public void handleStockLockedRelease(StockLockedTo lockedTo, Message message, Channel channel) throws IOException {
        System.out.println("收到解锁库存的消息");
        try{
            wareSkuService.unlockStock(lockedTo);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        }catch (Exception e){
            channel.basicReject(message.getMessageProperties().getDeliveryTag(), true);
        }
    }

    @RabbitHandler
    public void handleOrderCloseRelease(OrderTo orderTo, Message message, Channel channel) throws IOException {
        System.out.println("订单关闭，准备解锁库存");
        try{
            wareSkuService.unlockStock(orderTo);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        }catch (Exception e){
            channel.basicReject(message.getMessageProperties().getDeliveryTag(), true);
        }
    }
}
