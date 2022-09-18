package com.atguigu.gmall.list.receiver;

import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.list.service.SearchService;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ListReceiver {

    @Autowired
    private SearchService searchService;

    //  监听上架消息
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_GOODS_UPPER,durable = "true",autoDelete = "false"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_GOODS),
            key = {MqConst.ROUTING_GOODS_UPPER}
    ))
    public void goodsUpper(Long skuId, Message message, Channel channel){

        //  判断
        try {
            if (skuId!=null){
                searchService.upperGoods(skuId);
            }
        } catch (Exception e) {
            //  网络异常 --- 重试             channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
            //  重试次数要是达到了规定：则直接写入消息记录表
            //  采用：直接写入消息表 --- receiv_exception_msg

            e.printStackTrace();
        }
        //  手动确认：
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }

    //  商品下架：
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_GOODS_LOWER,durable = "true",autoDelete = "false"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_GOODS),
            key = {MqConst.ROUTING_GOODS_LOWER}
    ))
    public void goodsLower(Long skuId, Message message, Channel channel){

        //  判断
        try {
            if (skuId!=null){
                searchService.lowerGoods(skuId);
            }
        } catch (Exception e) {
            //  网络异常 --- 重试             channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
            //  重试次数要是达到了规定：则直接写入消息记录表
            //  采用：直接写入消息表 --- receiv_exception_msg

            e.printStackTrace();
        }
        //  手动确认：
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }
}
