package com.atguigu.gmall.order.controller;

import com.atguigu.gmall.cart.client.CartFeignClient;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.util.AuthContextHolder;
import com.atguigu.gmall.model.cart.CartInfo;
import com.atguigu.gmall.model.order.OrderDetail;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.user.UserAddress;
import com.atguigu.gmall.order.service.OrderService;
import com.atguigu.gmall.user.client.UserFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/order")
public class OrderApiController {

    @Autowired
    private UserFeignClient userFeignClient;

    @Autowired
    private CartFeignClient cartFeignClient;

    @Autowired
    private OrderService orderService;

    //  /api/order/auth/trade
    //  订单结算页面原创调用url
    @GetMapping("/auth/trade")
    public Result authTrade(HttpServletRequest request){
        //  获取用户Id
        String userId = AuthContextHolder.getUserId(request);
        //  创建一个map 集合
        HashMap<String, Object> hashMap = new HashMap<>();
        //  获取收货地址列表
        List<UserAddress> userAddressList = userFeignClient.findUserAddressListByUserId(userId);
        //  获取到订单明细集合 赋值 给 detailArrayList
        List<CartInfo> cartCheckedList = cartFeignClient.getCartCheckedList(userId);
        //  AtomicReference<Integer> totalNum = new AtomicReference<>(0);   Integer totalNum = 0;
        //  AtomicInteger totalNum = new AtomicInteger(0);  int totalNum = 0;
        AtomicInteger totalNum = new AtomicInteger();
        List<OrderDetail> detailArrayList = cartCheckedList.stream().map(cartInfo -> {
            //  声明一个订单明细
            OrderDetail orderDetail = new OrderDetail();
            //  赋值
            orderDetail.setSkuId(cartInfo.getSkuId());
            orderDetail.setSkuName(cartInfo.getSkuName());
            orderDetail.setSkuNum(cartInfo.getSkuNum());
            orderDetail.setImgUrl(cartInfo.getImgUrl());
            //  赋值实时价格
            orderDetail.setOrderPrice(cartInfo.getSkuPrice());
            //  计算总件数
            //  totalNum.updateAndGet(v -> v + cartInfo.getSkuNum());
            totalNum.addAndGet(cartInfo.getSkuNum());
            return orderDetail;
        }).collect(Collectors.toList());

        //  单价*数量：
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderDetailList(detailArrayList);
        orderInfo.sumTotalAmount();
        //  userAddressList， detailArrayList， totalNum， totalAmount， tradeNo==交易的流水号！ 防止用户无刷新重复提交订单！
        hashMap.put("userAddressList",userAddressList);
        hashMap.put("detailArrayList",detailArrayList);
        hashMap.put("totalNum",totalNum);// 总件数
        hashMap.put("totalAmount",orderInfo.getTotalAmount());// 总价格
        hashMap.put("tradeNo",this.orderService.getTradeNo(userId));// 总价格
        //  返回数据
        return Result.ok(hashMap);
    }

    //  保存订单  http://api.gmall.com/api/order/auth/submitOrder?tradeNo=34567rtyui
    //  auth/submitOrder
    @PostMapping("auth/submitOrder")
    public  Result submitOrder(@RequestBody OrderInfo orderInfo,HttpServletRequest request){
        //  获取用户Id
        String userId = AuthContextHolder.getUserId(request);
        orderInfo.setUserId(Long.parseLong(userId));

        //  获取到页面传递的流水号：
        String tradeNo = request.getParameter("tradeNo");
        //  调用比较方法
        Boolean result = this.orderService.checkTradeNo(tradeNo, userId);
        //  表示比较失败
        if (!result){
            return Result.fail().message("不能重复无刷新回退提交订单");
        }
        //  删除流水号
        this.orderService.delTradeNo(userId);

        //  在此需要校验库存
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        //  循环遍历
        //        orderDetailList.forEach(orderDetail -> {
        //            Long skuId = orderDetail.getSkuId();
        //            Integer skuNum = orderDetail.getSkuNum();
        //            //  调用校验库存系统接口
        //            Boolean exist = this.orderService.checkStock(skuId,skuNum);
        //            if (!exist){
        //                Result<Object> message = Result.fail().message(orderDetail.getSkuId() + "库存不足！");
        //                return message;
        //            }
        //        });
        for (OrderDetail orderDetail : orderDetailList) {
            Long skuId = orderDetail.getSkuId();
            Integer skuNum = orderDetail.getSkuNum();
            //  调用校验库存系统接口
            Boolean exist = this.orderService.checkStock(skuId,skuNum);
            if (!exist){
                Result<Object> message = Result.fail().message(orderDetail.getSkuId() + "库存不足！");
                return message;
            }
        }

        //  调用服务层方法
        Long orderId = this.orderService.saveOrderInfo(orderInfo);
        //  返回订单Id
        return Result.ok(orderId);
    }
}
