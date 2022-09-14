package com.atguigu.gmall.order.client;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.order.client.impl.OrderDegradeFeignClient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;

@FeignClient(value = "service-order",fallback = OrderDegradeFeignClient.class)
public interface OrderFeignClient {

    //  Result authTrade(HttpServletRequest request); 这样写就错了
    //  request  用来获取用户Id，但是，通过feign 远程调用的时候，不会携带头文件信息 header
    @GetMapping("/api/order/auth/trade")
    Result authTrade();
}
