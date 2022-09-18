package com.atguigu.gmall.payment.service;

import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.payment.PaymentInfo;

import java.util.Map;

public interface PaymentService {

    //  http://api.gmall.com/api/payment/alipay/submit/47

    /**
     * 保存交易记录
      * @param orderInfo    订单信息
     * @param paymentType   支付类型
     */
    void savePaymentInfo(OrderInfo orderInfo,String paymentType);

    /**
     * 获取交易记录
     * @param outTradeNo
     * @param paymentType
     * @return
     */
    PaymentInfo getPaymentInfo(String outTradeNo, String paymentType);

    /**
     *更新交易记录
     * @param outTradeNo
     * @param paymentType
     * @param paramsMap
     */
    void paySuccess(String outTradeNo, String paymentType, Map<String, String> paramsMap);
}
