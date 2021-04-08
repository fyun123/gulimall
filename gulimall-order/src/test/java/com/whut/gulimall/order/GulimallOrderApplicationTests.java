package com.whut.gulimall.order;

import com.whut.gulimall.order.entity.OmsRefundInfoEntity;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class GulimallOrderApplicationTests {

    @Autowired
    AmqpAdmin amqpAdmin;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Test
    void sendMsg(){
        OmsRefundInfoEntity omsRefundInfoEntity = new OmsRefundInfoEntity();
        omsRefundInfoEntity.setId(1L);
        omsRefundInfoEntity.setOrderReturnId(123L);
        omsRefundInfoEntity.setRefundContent("拍错了，申请退货");
        rabbitTemplate.convertAndSend("hello-java-direct","hello.java2",omsRefundInfoEntity);
    }

    @Test
    void createExchange() {
        DirectExchange directExchange = new DirectExchange("hello-java-direct",true,false);
        amqpAdmin.declareExchange(directExchange);
    }

    @Test
    void createQueue(){
        Queue queue = new Queue("hello-java-queue",true,false,false);
        amqpAdmin.declareQueue(queue);
    }

    @Test
    void createBinding(){
        Binding binding = new Binding("hello-java-queue",
                Binding.DestinationType.QUEUE,
                "hello-java-direct",
                "hello.java",null);
        amqpAdmin.declareBinding(binding);
    }
}
