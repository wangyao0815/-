package com.atguigu.gmall.product;

import com.atguigu.gmall.common.constant.RedisConst;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan({"com.atguigu.gmall"})
@EnableDiscoveryClient
public class ServiceProductApplication implements CommandLineRunner {

   @Autowired
   private RedissonClient redissonClient;

   public static void main(String[] args) {
      SpringApplication.run(ServiceProductApplication.class, args);
   }
   // 初始化方法
   @Override
   public void run(String... args) throws Exception {
      // 设置误判率，数据规模
      RBloomFilter<Object> bloomFilter = redissonClient.getBloomFilter(RedisConst.SKU_BLOOM_FILTER);
      bloomFilter.tryInit(10000,0.001);
   }
}
