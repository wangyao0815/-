package com.atguigu.gmall.mq.receiver;

import com.atguigu.gmall.mq.config.DeadLetterMqConfig;
import com.atguigu.gmall.mq.config.DelayedMqConfig;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Component
public class ConfirmReceiver {

    @Autowired
    private RedisTemplate redisTemplate;

    //  使用注解监听：
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "queue.confirm", durable = "true", autoDelete = "false"),
            exchange = @Exchange(value = "exchange.confirm"),
            key = {"routing.confirm"}
    ))
    public void getMsg(String msg, Message message, Channel channel) {
        //  如果有异常，则需要nack
        try {
            System.out.println("接收的消息:\t" + msg);
            System.out.println("接收的消息message:\t" + new String(message.getBody()));
        } catch (Exception e) {
            //  捕获异常：
            //  第一个参数表示消息标签：   第二个参数表示是否是批量接收  true：批量接收  false:单条接收  第三个参数表示：是否重回队列 true:重回，false:不重回
            //  网络异常：channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
            //  业务异常：重试是没有作用！无限死循环；设置次数3！如果重试次数已到！则需要做消息表！  将这个  消息直接写入table 中！
            //  只要有异常，直接进入消息记录表！
            //  channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
            e.printStackTrace();
        }

        //  是否需要接收？
        //  第一个参数表示消息标签：   第二个参数表示是否是批量接收  true：批量接收  false:单条接收
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }

    //  监听延迟消息
    @SneakyThrows
    @RabbitListener(queues = DeadLetterMqConfig.queue_dead_2)
    public void getMsg2(String msg, Message message, Channel channel){
        //  打印接收消息的时间
        //  声明一个时间格式
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        System.out.println("接收时间：\t"+simpleDateFormat.format(new Date())+msg);
        //  第一个参数表示消息标签：   第二个参数表示是否是批量接收  true：批量接收  false:单条接收
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }

    //  基于插件
    @SneakyThrows
    @RabbitListener(queues = DelayedMqConfig.queue_delay_1)
    public void getMsg3(String msg, Message message, Channel channel){
        //  setnx   1. 根据value 0 或 1 判断是否可以继续消费。  2. 如果消费失败了，则直接删除key;
        Boolean result = this.redisTemplate.opsForValue().setIfAbsent("lock", "0", 1, TimeUnit.MINUTES);
        //  result = true 说明没有人消费过。 result = false 说明有人消费过.
        if (!result){
            //  确认签收
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
            return;
        }
        //  打印接收消息的时间：
        //  声明一个时间格式
        try {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            System.out.println("接收时间：\t"+simpleDateFormat.format(new Date()) + msg);
        } catch (Exception e) {
            //  如果有异常，则直接删除.
            this.redisTemplate.delete("lock");
            e.printStackTrace();
        }
        //  第一个参数表示消息标签：    第二参数表示是否是批量签收  true: 批量签收， false: 单条签收 -- 忘记
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);

    }
}
