package com.atguigu.gmall.order.client.impl;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.order.client.OrderFeignClient;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

@Component
public class OrderDegradeFeignClient implements OrderFeignClient {
    @Override
    public Result authTrade() {
        return null;
    }

    @Override
    public OrderInfo getOrderInfo(Long orderId) {
        return null;
    }
}
