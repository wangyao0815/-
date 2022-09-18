package com.atguigu.gmall.common.config;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.model.GmallCorrelationData;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.C;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class MQProducerAckConfig implements RabbitTemplate.ConfirmCallback, RabbitTemplate.ReturnCallback {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RedisTemplate redisTemplate;

    //  初始化
    @PostConstruct
    public void init(){
        rabbitTemplate.setConfirmCallback(this);
        rabbitTemplate.setReturnCallback(this);
    }
    /**
     * 判断这个消息是否到了交换机
     * @param correlationData   封装一个消息，这个消息有自定义的Id 标识
     * @param ack                 true：消息到了交换机 false：没有到交换机
     * @param codeMsg                 原因：
     */
    @Override
    public void confirm(CorrelationData correlationData, boolean ack, String codeMsg) {
        if (ack){
            log.info("消息到交换机");
            System.out.println(codeMsg);
        }else {
            log.error("消息没有到交换机");
            System.out.println(codeMsg);

            //  调用重试方法
            this.retrySendMsg(correlationData);
        }
    }

    /**
     *  判断这个消息是否到队列 ---- 消息没有到队列的时候才会执行这个方法
     * @param message       消息主体
     * @param replyCode     应答码
     * @param replyText     原因
     * @param exchange      交换器
     * @param routingKey    路由键
     */
    @Override
    public void returnedMessage(Message message, int replyCode, String replyText, String exchange, String routingKey) {
        // 反序列化对象输出
        System.out.println("消息主体: " + new String(message.getBody()));
        System.out.println("应答码: " + replyCode);
        System.out.println("描述：" + replyText);
        System.out.println("消息使用的交换器 exchange : " + exchange);
        System.out.println("消息使用的路由键 routing : " + routingKey);

        //  判断如果是属于插件实现的延迟消息。则不需要重试.
        //        if ("exchange.delay".equals(exchange) && "routing.delay".equals(routingKey)){
        //            return;
        //        }
        //  消息没有到队列的时候会走这个方法，找到对应的key 获取value 数据
        String correlationDataId = (String) message.getMessageProperties().getHeaders().get("spring_returned_message_correlation");
        //  从缓存中获取数据
        String strJson = (String) this.redisTemplate.opsForValue().get(correlationDataId);
        //  转化为对象
        GmallCorrelationData gmallCorrelationData = JSON.parseObject(strJson, GmallCorrelationData.class);
        //  调用重试方法
        this.retrySendMsg(gmallCorrelationData);
    }

    /**
     * 自定义重试发送消息方法
     * @param correlationData
     */
    private void retrySendMsg(CorrelationData correlationData) {
        //  什么时候重试：次数没有超过3！
        GmallCorrelationData gmallCorrelationData = (GmallCorrelationData) correlationData;

        //  获取到了重试次数
        int retryCount = gmallCorrelationData.getRetryCount();
        System.out.println("重试次数："+retryCount);
        //  retryCount = 0; 0 1 2
        if (retryCount>=3){
            //  不需要重试，直接将数据写入消息表！ send_exception_msg
            log.error("重试次数已到！将数据持久化");
        }else {
            //  变量迭代：
            retryCount++;
            //  写回缓存
            gmallCorrelationData.setRetryCount(retryCount);
            //  覆盖缓存中的数据
            redisTemplate.opsForValue().set(gmallCorrelationData.getId(), JSON.toJSONString(gmallCorrelationData),10, TimeUnit.MINUTES);
            //  判断这个消息的类型
            if (gmallCorrelationData.isDelay()){
                //  属于延迟消息
                this.rabbitTemplate.convertAndSend(gmallCorrelationData.getExchange(),gmallCorrelationData.getRoutingKey(),gmallCorrelationData.getMessage(),(message)->{
                    //  设置延迟时间：
                    message.getMessageProperties().setDelay(gmallCorrelationData.getDelayTime()*1000);
                    return message;
                },gmallCorrelationData);
            }else {
                //  重试：再次发送消息，要使用redisTemplate 发送消息
                //  发送数据的时候
                this.rabbitTemplate.convertAndSend(gmallCorrelationData.getExchange(), gmallCorrelationData.getRoutingKey(),gmallCorrelationData.getMessage(),gmallCorrelationData);
            }


        }
    }
}
