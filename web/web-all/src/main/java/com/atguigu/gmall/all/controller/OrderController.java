package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.order.client.OrderFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

@Controller
public class OrderController {

    @Autowired
    private OrderFeignClient orderFeignClient;

    //  http://order.gmall.com/trade.html
    @GetMapping("trade.html")
    public String trade(Model model){
        //  userAddressList， detailArrayList， totalNum， totalAmount， tradeNo==交易的流水号！ 防止用户无刷新重复提交订单！
        //  model.addAttribute("userAddressList",userAddressList);
        //  model.addAllAttributes(map)
        Result<Map> result = orderFeignClient.authTrade();
        model.addAllAttributes(result.getData());
        //  返回订单结算页面
        return "order/trade";
    }

    //  我的订单
    @GetMapping("myOrder.html")
    public String myOrder(){

        //  返回我的订单页面
        return "order/myOrder";
    }
}
