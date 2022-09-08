package com.atguigu.gmall.item.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
public class ThreadPoolExecutorConfig {

    //  7个核心参数；
    @Bean
    public ThreadPoolExecutor threadPoolExecutor(){
        //  创建线程池
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                5,  //  核心线程数
                100, // 最大线程数
                3, //   空闲线程存活时间
                TimeUnit.SECONDS,   // 时间单位
                new ArrayBlockingQueue<>(3), // 阻塞队列
                Executors.defaultThreadFactory(),   //  线程工厂
                new ThreadPoolExecutor.AbortPolicy() // 拒绝策略 抛出异常 ，由调用者机制，抛弃等待时间最久的任务，直接丢弃
        );
        return threadPoolExecutor;
    }
}
