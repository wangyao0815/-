package com.atguigu.gmall.payment.controller;

import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import com.atguigu.gmall.model.enums.PaymentType;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.payment.config.AlipayConfig;
import com.atguigu.gmall.payment.service.AlipayService;
import com.atguigu.gmall.payment.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Controller //@ResponseBody + @Controller  @ResponseBody -- 1.返回json  2.直接将数据输出到页面
@RequestMapping("/api/payment/alipay")
public class AlipayController {

    @Autowired
    private AlipayService alipayService;

    @Autowired
    private PaymentService paymentService;

    @Value("${app_id}")
    private String app_id;

    @Autowired
    private RedisTemplate redisTemplate;

    //    http://api.gmall.com/api/payment/alipay/submit/78
    @GetMapping("submit/{orderId}")
    @ResponseBody
    public String aliPay(@PathVariable Long orderId) {
        //  调用服务层方法
        String form = alipayService.createaliPay(orderId);
        //  返回数据
        return form;
    }

    //  同步回调：让用户看到支付成功页面，能够继续后续操作.
    //  http://api.gmall.com/api/payment/alipay/callback/return
    @RequestMapping("callback/return")
    public String callBack() {
        //  重定向到web-all 模块中的控制器！
        //  http://payment.gmall.com/pay/success.html
        return "redirect:" + AlipayConfig.return_order_url;
    }

    //  异步通知：http://rjsh38.natappfree.cc/api/payment/alipay/callback/notify
    //  https: //商家网站通知地址?voucher_detail_list=[{"amount":"0.20","merchantContribute":"0.00","name":"5折券","otherContribute":"0.20","type":"ALIPAY_DISCOUNT_VOUCHER","voucherId":"2016101200073002586200003BQ4"}]&fund_bill_list=[{"amount":"0.80","fundChannel":"ALIPAYACCOUNT"},{"amount":"0.20","fundChannel":"MDISCOUNT"}]&subject=PC网站支付交易&trade_no=2016101221001004580200203978&gmt_create=2016-10-12 21:36:12&notify_type=trade_status_sync&total_amount=1.00&out_trade_no=mobile_rdm862016-10-12213600&invoice_amount=0.80&seller_id=2088201909970555&notify_time=2016-10-12 21:41:23&trade_status=TRADE_SUCCESS&gmt_payment=2016-10-12 21:37:19&receipt_amount=0.80&passback_params=passback_params123&buyer_id=2088102114562585&app_id=2016092101248425&notify_id=7676a2e1e4e737cff30015c4b7b55e3kh6& sign_type=RSA2&buyer_pay_amount=0.80&sign=***&point_amount=0.00
    @PostMapping("callback/notify")
    @ResponseBody
    public String callbackNotify(@RequestParam Map<String, String> paramsMap){
        //  获取到out_trade_no 数据
        String outTradeNo = paramsMap.get("out_trade_no");
        //  支付的金额
        String totalAmount = paramsMap.get("total_amount");
        String appId = paramsMap.get("app_id");
        String tradeStatus = paramsMap.get("trade_status");
        String notifyId = paramsMap.get("notify_id");

        boolean signVerified = false; //调用SDK验证签名
        try {
            signVerified = AlipaySignature.rsaCheckV1(paramsMap, AlipayConfig.alipay_public_key, AlipayConfig.charset, AlipayConfig.sign_type);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }

        if(signVerified){
            // TODO 验签成功后，按照支付结果异步通知中的描述，对支付结果中的业务内容进行二次校验，校验成功后在response中返回success并继续商户自身业务处理，校验失败返回failure
            // 根据outTradeNo ,PaymentType 获取到 paymentInfo;
            PaymentInfo paymentInfoQuery = this.paymentService.getPaymentInfo(outTradeNo, PaymentType.ALIPAY.name());
            //  判断
            //  细节：支付金额 0.01
            if (paymentInfoQuery == null || new BigDecimal("0.01").compareTo(new BigDecimal(totalAmount))!=0
                    || !appId.equals(app_id)){
                //  返回失败
                return "failure";
            }
            //  setnx key value;  24*60 = 1440 + 22
            Boolean result = this.redisTemplate.opsForValue().setIfAbsent(notifyId, "0", 1462, TimeUnit.MINUTES);
            if (!result){
                //  说明有人发送过通知！
                return "failure";
            }
            //  判断 状态：
            if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)){
                //  比较成功之后，更新交易状态： trade_no payment_status callback_time callback_content
                this.paymentService.paySuccess(outTradeNo, PaymentType.ALIPAY.name(),paramsMap);
                return "success";
            }
        }else{
            // TODO 验签失败则记录异常日志，并在response中返回failure.
            return "failure";
        }
        return "failure";
    }
}
