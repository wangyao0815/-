package com.atguigu.gmall.mq.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.mq.config.DeadLetterMqConfig;
import com.atguigu.gmall.mq.config.DelayedMqConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.util.Date;

@RestController
@RequestMapping("mq")
public class MqController {

    @Autowired
    private RabbitService rabbitService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    //  封装发送消息的控制器
    @GetMapping("sendMsg")
    public Result sendMsg(){
        rabbitService.sendMsg("exchange.confirm", "routing666.confirm", "来人了，开始接客吧！");
        return Result.ok();
    }

    //  发送消息
    @GetMapping("sendDeadLettle")
    public Result sendDeadLettle(){
        //  声明一个时间格式
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        System.out.println("发送时间：\t"+simpleDateFormat.format(new Date()));
        //  调用发送想消息方法
        rabbitService.sendMsg(DeadLetterMqConfig.exchange_dead, DeadLetterMqConfig.routing_dead_1, "来人了，开始接客吧.延迟了......");
        return Result.ok();
    }

    //  发送消息：
    @GetMapping("sendDelayMsg")
    public Result sendDelayMsg(){
        //  声明一个时间格式
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        // 使用原生的发送消息
        this.rabbitTemplate.convertAndSend(DelayedMqConfig.exchange_delay,DelayedMqConfig.routing_delay,"atguigu",(message)->{
            //  设置延迟时间 10s
            System.out.println("发送时间：\t"+simpleDateFormat.format(new Date()));
            message.getMessageProperties().setDelay(10000);
            return message;
        });
        rabbitService.sendDelayMsg(DelayedMqConfig.exchange_delay,DelayedMqConfig.routing_delay,"atguigu",3);
        return Result.ok();
    }
}
