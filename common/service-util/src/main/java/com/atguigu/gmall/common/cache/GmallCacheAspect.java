package com.atguigu.gmall.common.cache;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.constant.RedisConst;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import springfox.documentation.spring.web.json.Json;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Component
@Aspect
public class GmallCacheAspect {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Around("@annotation(com.atguigu.gmall.common.cache.GmallCache)")
    public Object GmallCacheAspectTest(ProceedingJoinPoint joinPoint) throws Throwable {
        //声明一个对象
        Object obj = null;
        /*
            1. 获取缓存的key key = 注解的前缀：方法的参数
                    a.  先获取注解

            2.通过key 来获取缓存中的数据
                true:
                    return object;
                false:
                    上锁：
                    查询数据  放入缓存：
                    解锁：
         */
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        GmallCache gmallCache = methodSignature.getMethod().getAnnotation(GmallCache.class);
        String prefix = gmallCache.prefix();
        // 组成key
        Object[] args = joinPoint.getArgs();
        String key = prefix + Arrays.asList(args).toString();

        try {
            //通过key 来获取数据
            //Object o = this.redisTemplate.opsForValue().get(key);  封装到一个方法中
            // 后续还需要讲具体的字符串变为具体的数据类型
            obj = this.getRedisData(key,methodSignature);
            if (obj==null){
                //锁的key
                String lockKey = key + ":lock";
                RLock lock = this.redissonClient.getLock(lockKey);
                //  上锁
                boolean res = lock.tryLock(RedisConst.SKULOCK_EXPIRE_PX1, RedisConst.SKULOCK_EXPIRE_PX2, TimeUnit.SECONDS);
                // 判断
                if (res){
                    try {
                        //查询数据库  执行原有方法 执行方法体
                        obj = joinPoint.proceed(args);
                        if (obj==null){
                            Object o = new Object();
                            // 有个问题
                            this.redisTemplate.opsForValue().set(key, JSON.toJSONString(o), RedisConst.SKUKEY_TEMPORARY_TIMEOUT, TimeUnit.SECONDS);
                            return o;
                        }
                        // 放入正常数据
                        this.redisTemplate.opsForValue().set(key, JSON.toJSONString(obj), RedisConst.SKUKEY_TIMEOUT, TimeUnit.SECONDS);
                        return obj;
                    } finally {
                        // 解锁
                        lock.unlock();
                    }
                }else{
                    //  等待
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    return GmallCacheAspectTest(joinPoint);
                }
            }else {
                return obj;
            }
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        //  查询数据库
        return joinPoint.proceed(args);
    }

    /**
     * 获取缓存数据
     * @param key
     * @param methodSignature
     * @return
     */
    private Object getRedisData(String key, MethodSignature methodSignature) {
        // get key
        String strJson = (String) this.redisTemplate.opsForValue().get(key);
        if (!StringUtils.isEmpty(strJson)){
            // 将这个字符串转换为具体的数据类型
            return JSON.parseObject(strJson,methodSignature.getReturnType());
        }
        // 默认返回空
        return null;
    }
}
