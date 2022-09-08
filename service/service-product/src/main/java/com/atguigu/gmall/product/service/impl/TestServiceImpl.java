package com.atguigu.gmall.product.service.impl;

import com.atguigu.gmall.product.service.TestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class TestServiceImpl implements TestService {

    @Autowired
    private StringRedisTemplate redisTemplate;



    @Override
    public synchronized void testLock() {
        // get num ;
        String numValue = redisTemplate.opsForValue().get("num");

        // 判断
        if (StringUtils.isEmpty(numValue)){
            return;
        }

        // 如果有数据 +1 写入缓存
        int num = Integer.parseInt(numValue);

        this.redisTemplate.opsForValue().set("num", String.valueOf(++num));
    }
}
