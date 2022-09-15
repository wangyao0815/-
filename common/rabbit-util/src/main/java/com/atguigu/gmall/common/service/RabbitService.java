package com.atguigu.gmall.common.service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RabbitService {

    //  beans.xml <bean class = "com.atguigu.pojo.Stu">
    //  使用的是模板设计模式？BeanFactory:是工厂？@Bean 单例模式，适配模式，aop 代理模式
    @Autowired
    private RabbitTemplate rabbitTemplate;

    //  发送消息
    public Boolean sendMsg(String exchange, String routingKey, Object msg){
        this.rabbitTemplate.convertAndSend(exchange, routingKey, msg);
        //  默认返回true
        return true;
    }
}
