package com.atguigu.gmall.order.receiver;

import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.order.service.OrderService;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OrderReceiver {

    @Autowired
    private OrderService orderService;

    //  监听消息：
    @SneakyThrows
    @RabbitListener(queues = MqConst.QUEUE_ORDER_CANCEL)
    public void orderCancel(Long orderId, Message message, Channel channel){
        try {
            //  判断
            if (orderId!=null){
                //  根据订单Id 获取订单对象
                OrderInfo orderInfo = orderService.getById(orderId);
                if (orderInfo!=null && "UNPAID".equals(orderInfo.getOrderStatus()) && "UNPAID".equals(orderInfo.getProcessStatus())){
                    //  更新订单的状态 CLOSED;
                    orderService.execExpiredOrder(orderId);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        //  手动确认
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }
}
