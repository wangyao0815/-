package com.atguigu.gmall.common.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.model.GmallCorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class RabbitService {

    //  beans.xml <bean class = "com.atguigu.pojo.Stu">
    //  使用的是模板设计模式？BeanFactory:是工厂？@Bean 单例模式，适配模式，aop 代理模式
    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    //  发送消息
    public Boolean sendMsg(String exchange, String routingKey, Object msg){
        //  发送消息之前，先将发送的交换机，路由键，消息本身内容，
        GmallCorrelationData gmallCorrelationData = new GmallCorrelationData();
        String correlationDataId = UUID.randomUUID().toString();
        gmallCorrelationData.setId(correlationDataId);  //  唯一标识
        gmallCorrelationData.setExchange(exchange);
        gmallCorrelationData.setRoutingKey(routingKey);
        gmallCorrelationData.setMessage(msg);

        //  将这个实体类先存储到缓存中
        redisTemplate.opsForValue().set(correlationDataId, JSON.toJSONString(gmallCorrelationData),10, TimeUnit.MINUTES);
        //  发送消息
        //  this.rabbitTemplate.convertAndSend(exchange, routingKey, msg);
        this.rabbitTemplate.convertAndSend(exchange, routingKey, msg,gmallCorrelationData);
        //  默认返回true
        return true;
    }

    //  封装一个发送延迟消息的方法.
    public Boolean sendDelayMsg(String exchange ,String routingKey, Object msg, int delayTime){
        //  发送消息的时候，也可能产生不会到交换机的情况，因此需要做重试处理. 利用 gmallCorrelationData
        GmallCorrelationData gmallCorrelationData = new GmallCorrelationData();
        String correlationDataId = UUID.randomUUID().toString();
        gmallCorrelationData.setId(correlationDataId);  //  唯一标识！
        gmallCorrelationData.setExchange(exchange);
        gmallCorrelationData.setRoutingKey(routingKey);
        gmallCorrelationData.setMessage(msg);
        //  gmallCorrelationData.setRetryCount(3);
        //  赋值延迟消息
        gmallCorrelationData.setDelay(true);
        gmallCorrelationData.setDelayTime(delayTime);

        //  将这个消息写入缓存.
        redisTemplate.opsForValue().set(correlationDataId, JSON.toJSONString(gmallCorrelationData),10, TimeUnit.MINUTES);

        //  发送延迟消息：
        this.rabbitTemplate.convertAndSend(exchange,routingKey,msg,(message)->{
            //  设置延迟时间：
            message.getMessageProperties().setDelay(delayTime*1000);
            return message;
        },gmallCorrelationData);
        return true;

    }
}
