package com.atguigu.gmall.mq.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.service.RabbitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("mq")
public class MqController {

    @Autowired
    private RabbitService rabbitService;

    //  封装发送消息的控制器
    @GetMapping("sendMsg")
    public Result sendMsg(){
        rabbitService.sendMsg("exchange.confirm", "routing.confirm", "来人了，开始接客吧！");
        return Result.ok();
    }
}
