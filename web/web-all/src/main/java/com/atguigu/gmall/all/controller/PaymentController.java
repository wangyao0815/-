package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.order.client.OrderFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;

@Controller
public class PaymentController {

    @Autowired
    private OrderFeignClient orderFeignClient;

    //  http://payment.gmall.com/pay.html?orderId=40
    @GetMapping("pay.html")
    public String pay(HttpServletRequest request){
        //  获取到订单Id
        String orderId = request.getParameter("orderId");
        OrderInfo orderInfo = orderFeignClient.getOrderInfo(Long.parseLong(orderId));
        request.setAttribute("orderInfo", orderInfo);
        //  返回支付页面信息
        return "payment/pay";
    }

    //  支付成功之后回跳控制器
    @GetMapping("pay/success.html")
    public String paySuccess(){
        //  支付成功页面.
        return "payment/success";
    }
}
