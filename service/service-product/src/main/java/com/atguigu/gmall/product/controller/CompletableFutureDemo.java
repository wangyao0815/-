package com.atguigu.gmall.product.controller;

import java.util.concurrent.*;

public class CompletableFutureDemo {
    //  主入口；
    public static void main(String[] args) throws ExecutionException, InterruptedException {

        //  创建一个有返回值的对象
        //        CompletableFuture<Void> completableFuture = CompletableFuture.runAsync(() -> {
        //            System.out.println("死鬼你回来了...");
        //        });

        //        CompletableFuture<Integer> integerCompletableFuture = CompletableFuture.supplyAsync(() -> {
        //            System.out.println("呵呵");
        //            int i = 1/0;
        //            return 1024;
        //        }).whenComplete((t,u)->{
        //            System.out.println("t:\t"+t);
        //            System.out.println("u:\t"+u);
        //        }).exceptionally((f)->{
        //            System.out.println("f:\t"+f);
        //            return 404;
        //        });
        //
        //        //  System.out.println(completableFuture.get());
        //        System.out.println(integerCompletableFuture.get()); // 放在最后执行，并且只调用一次！

        //  串行：
        //        CompletableFuture<Integer> integerCompletableFuture = CompletableFuture.supplyAsync(() -> {
        //            System.out.println("串行");
        //            //  int i = 1/0;
        //            return 1024;
        //        }).thenApply((fn)->{
        //            System.out.println("fn:\t"+fn);
        //            return fn*2;
        //        }).whenComplete((t,u)->{
        //            System.out.println("t:\t"+t);
        //            System.out.println("u:\t"+u);
        //        }).exceptionally((f)->{
        //            System.out.println("f:\t"+f);
        //            return 404;
        //        });

        //  讲一下如何创建线程池的？
        //  四种： 1.一池一线程 2.固定大小的线程池 3. 可扩容线程池 通过工具类创建！   不用 为什么?  因为阻塞队列长度，或最大线程个数是Integer.MAX_VALUE 因为会发生OOM ！
        //  所以都使用 自定义线程池：
        //  7个核心参数；
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                3,  //  核心线程数
                100, // 最大线程数
                3, //   空闲线程存活时间
                TimeUnit.SECONDS,   // 时间单位
                new ArrayBlockingQueue<>(3), // 阻塞队列
                Executors.defaultThreadFactory(),   //  线程工厂
                new ThreadPoolExecutor.AbortPolicy() // 拒绝策略 抛出异常 ，由调用者机制，抛弃等待时间最久的任务，直接丢弃
        );
        //  核心线程个数如何设置?  io密集型：2n  cpu 密集型：n+1  n: 内核数
        //  线程池能够处理最大任务是多少? 最大线程数+阻塞队列个数
        //  线程池工作原理：   拒绝策略jdk4种，可以自定义拒绝策略！
        //  你工作用了么？ 用！ 你们线上核心线程个数是多少? 33 65

        //  并行：
        CompletableFuture<String> completableFutureA = CompletableFuture.supplyAsync(() -> {
            return "hello";
        },threadPoolExecutor);

        //  创建B
        CompletableFuture<Void> completableFutureB = completableFutureA.thenAcceptAsync((c) -> {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println(c + ":\tB");
        },threadPoolExecutor);

        //  创建C
        CompletableFuture<Void> completableFutureC = completableFutureA.thenAcceptAsync((c) -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println(c + ":\tC");
        },threadPoolExecutor);

        System.out.println(completableFutureB.get());
        System.out.println(completableFutureC.get());

    }
}
