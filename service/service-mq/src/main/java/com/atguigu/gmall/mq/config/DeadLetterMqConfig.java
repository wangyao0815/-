package com.atguigu.gmall.mq.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;

/**
 * 描述：这个配置文件：将交换机和队列进行初始化
 */
@Configuration
public class DeadLetterMqConfig {

    //  声明一些变量
    public static final String exchange_dead = "exchange.dead";
    public static final String routing_dead_1 = "routing.dead.1";
    public static final String routing_dead_2 = "routing.dead.2";
    public static final String queue_dead_1 = "queue.dead.1";
    public static final String queue_dead_2 = "queue.dead.2";

    //  设置绑定关系
    @Bean
    public DirectExchange exchange(){
        //  不需要处理：其他参数
        return new DirectExchange(exchange_dead,true,false);
    }

    //  设置队列1
    @Bean
    public Queue queue(){
        //  声明map 集合
        HashMap<String, Object> hashMap = new HashMap<>();
        //  10秒过期时间
        hashMap.put("x-message-ttl", 10000);
        //  指定交换机与队列的绑定关系
        hashMap.put("x-dead-letter-exchange", exchange_dead);
        //  通过路由键2 绑定到队列2
        hashMap.put("x-dead-letter-routing-key", routing_dead_2);
        return new Queue(queue_dead_1,true,false,false,hashMap);
    }

    //  设置绑定关系
    @Bean
    public Binding binding(){
        //  通过路由键1，与绑定key 1 绑定到队列1
        return BindingBuilder.bind(queue()).to(exchange()).with(routing_dead_1);
    }

    //  设置队列2
    @Bean
    public Queue queue2(){
        return new Queue(queue_dead_2,true,false,false);
    }

    //  设置绑定关系
    @Bean
    public Binding binding2(){
        //  通过路由键1，与绑定key 1 绑定到队列1
        return BindingBuilder.bind(queue2()).to(exchange()).with(routing_dead_2);
    }
}
