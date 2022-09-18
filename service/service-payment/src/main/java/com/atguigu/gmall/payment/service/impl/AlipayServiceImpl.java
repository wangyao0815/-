package com.atguigu.gmall.payment.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.atguigu.gmall.model.enums.PaymentType;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.order.client.OrderFeignClient;
import com.atguigu.gmall.payment.config.AlipayConfig;
import com.atguigu.gmall.payment.service.AlipayService;
import com.atguigu.gmall.payment.service.PaymentService;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Calendar;

@Service
public class AlipayServiceImpl implements AlipayService {

    @Autowired
    private AlipayClient alipayClient;

    @Autowired
    private OrderFeignClient orderFeignClient;

    @Autowired
    private PaymentService paymentService;
    @Override
    public String createaliPay(Long orderId) {

        //  根据订单Id 获取订单对象
        OrderInfo orderInfo = this.orderFeignClient.getOrderInfo(orderId);

        //  保存交易记录
        this.paymentService.savePaymentInfo(orderInfo, PaymentType.ALIPAY.name());

        //  判断订单状态
        if ("CLOSED".equals(orderInfo.getOrderStatus()) || "PAID".equals(orderInfo.getOrderStatus())){
            return "订单已关闭或已支付!";
        }

        //  如果支付宝与微信只能二选一：借助redis stenx 命令
        //  支付宝与微信可以同时支付：   退款借口

        //  AlipayClient alipayClient =  new DefaultAlipayClient( "https://openapi.alipay.com/gateway.do" , APP_ID, APP_PRIVATE_KEY, FORMAT, CHARSET, ALIPAY_PUBLIC_KEY, SIGN_TYPE);  //获得初始化的AlipayClient
        AlipayTradePagePayRequest alipayRequest =  new  AlipayTradePagePayRequest(); //创建API对应的request
        //  设置同步回调  http://api.gmall.com/api/payment/alipay/callback/return
        alipayRequest.setReturnUrl(AlipayConfig.return_payment_url);
        //  设置异步回调  http://rjsh38.natappfree.cc/api/payment/alipay/callback/notify
        alipayRequest.setNotifyUrl( AlipayConfig.notify_payment_url ); //在公共参数中设置回跳和通知地址

        //  第一种方式
        JSONObject jsonObject = new JSONObject();
        //  商户订单号
        jsonObject.put("out_trade_no",orderInfo.getOutTradeNo());
        jsonObject.put("total_amount","0.01");
        jsonObject.put("subject",orderInfo.getTradeBody());
        jsonObject.put("product_code","FAST_INSTANT_TRADE_PAY");
        //  10m以后 设置二维码的过期时间：绝对时间：yyyy-MM-dd HH:mm:ss
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        //  时间计算：
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE,10);
        jsonObject.put("time_expire",simpleDateFormat.format(calendar.getTime()));
        //  赋值操作
        alipayRequest.setBizContent(jsonObject.toJSONString());
        //  第二种方式：可以设置map

        String form= "" ;
        try  {
            form = alipayClient.pageExecute(alipayRequest).getBody();  //调用SDK生成表单
        }  catch  (AlipayApiException e) {
            e.printStackTrace();
        }
        return form;
    }

}
