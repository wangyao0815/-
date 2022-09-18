package com.atguigu.gmall.order.service;

import com.atguigu.gmall.model.order.OrderInfo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.apache.ibatis.annotations.Param;

public interface OrderService extends IService<OrderInfo> {
    //  保存订单
    Long saveOrderInfo(OrderInfo orderInfo);
    //  返回流水号
    String getTradeNo(String userId);
    //  比较流水号   tradeNo: 页面流水号  userId：获取缓存的流水号
    Boolean checkTradeNo(String tradeNo,String userId);
    //  删除流水号
    void delTradeNo(String userId);
    //  检验库存系统
    Boolean checkStock(Long skuId, Integer skuNum);
    //  查看我的订单
    IPage<OrderInfo> getMyOrderList(Page<OrderInfo> orderInfoPage, String userId);
    //  取消订单
    void execExpiredOrder(Long orderId);
    //  根据订单Id 获取订单信息
    OrderInfo getOrderInfo(Long orderId);
}
