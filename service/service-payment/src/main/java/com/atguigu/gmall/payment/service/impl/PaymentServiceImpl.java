package com.atguigu.gmall.payment.service.impl;

import com.atguigu.gmall.model.enums.PaymentStatus;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.payment.mapper.PaymentInfoMapper;
import com.atguigu.gmall.payment.service.PaymentService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;

@Service
public class PaymentServiceImpl implements PaymentService {

    //  引出mapper
    @Autowired
    private PaymentInfoMapper paymentInfoMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public void savePaymentInfo(OrderInfo orderInfo,String paymentType) {
        //   查询是否存在
        QueryWrapper<PaymentInfo> paymentInfoQueryWrapper = new QueryWrapper<>();
        paymentInfoQueryWrapper.eq("order_id", orderInfo.getId());
        paymentInfoQueryWrapper.eq("payment_type", paymentType);
        PaymentInfo paymentInfoQuery = paymentInfoMapper.selectOne(paymentInfoQueryWrapper);
        if (paymentInfoQuery!=null){
            return;
        }
        //  声明对象
        PaymentInfo paymentInfo = new PaymentInfo();
        //  给paymentInfo 赋值
        paymentInfo.setOutTradeNo(orderInfo.getOutTradeNo());
        paymentInfo.setOrderId(orderInfo.getId());
        paymentInfo.setUserId(orderInfo.getUserId());
        paymentInfo.setPaymentStatus(paymentType);
        //  paymentInfo.setTradeNo();   支付完成之后回调的时候获取
        paymentInfo.setTotalAmount(orderInfo.getTotalAmount());
        paymentInfo.setSubject(orderInfo.getTradeBody());
        paymentInfo.setPaymentStatus(PaymentStatus.UNPAID.name());
        //  保存数据
        paymentInfoMapper.insert(paymentInfo);

    }

    @Override
    public PaymentInfo getPaymentInfo(String outTradeNo, String paymentType) {
        //  设置更新条件
        QueryWrapper<PaymentInfo> paymentInfoQueryWrapper = new QueryWrapper<>();
        paymentInfoQueryWrapper.eq("out_trade_no", outTradeNo);
        paymentInfoQueryWrapper.eq("payment_type", paymentType);
        PaymentInfo paymentInfo = paymentInfoMapper.selectOne(paymentInfoQueryWrapper);
        if (paymentInfo == null){
            return null;
        }
        return paymentInfo;
    }

    @Override
    public void paySuccess(String outTradeNo, String paymentType, Map<String, String> paramsMap) {

        try {
            //  创建一个paymentInfo对象
            PaymentInfo paymentInfo = new PaymentInfo();
            //  trade_no payment_status callback_time callback_content
            paymentInfo.setTradeNo(paramsMap.get("trade_no"));
            paymentInfo.setPaymentStatus(PaymentStatus.PAID.name());
            paymentInfo.setCallbackTime(new Date());
            paymentInfo.setCallbackContent(paramsMap.toString());

            //  设置更新条件
            QueryWrapper<PaymentInfo> paymentInfoQueryWrapper = new QueryWrapper<>();
            paymentInfoQueryWrapper.eq("out_trade_no", outTradeNo);
            paymentInfoQueryWrapper.eq("payment_type", paymentType);
            //  更新数据
            paymentInfoMapper.update(paymentInfo,paymentInfoQueryWrapper);
        } catch (Exception e) {
            //  如果有异常则删除key
            this.redisTemplate.delete(paramsMap.get("notify_id"));
            e.printStackTrace();
        }
    }
}
