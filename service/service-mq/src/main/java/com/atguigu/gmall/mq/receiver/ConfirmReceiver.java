package com.atguigu.gmall.mq.receiver;

import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ConfirmReceiver {

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
}
