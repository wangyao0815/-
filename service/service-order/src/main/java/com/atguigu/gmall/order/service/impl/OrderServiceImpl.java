package com.atguigu.gmall.order.service.impl;

import com.atguigu.gmall.common.util.HttpClientUtil;
import com.atguigu.gmall.model.enums.OrderStatus;
import com.atguigu.gmall.model.enums.ProcessStatus;
import com.atguigu.gmall.model.order.OrderDetail;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.order.mapper.OrderDetailMapper;
import com.atguigu.gmall.order.mapper.OrderInfoMapper;
import com.atguigu.gmall.order.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Value("${ware.url}")
    private String wareUrl;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveOrderInfo(OrderInfo orderInfo) {
        //  order_info total_amount order_status user_id out_trade_no trade_body operate_time expire_time process_status
        orderInfo.sumTotalAmount(); // total_amount
        //  order_status
        orderInfo.setOrderStatus(OrderStatus.UNPAID.name());
        //  第三方交易编号,必须保证不能重复
        String outTradeNo = "ATGUIGU" + System.currentTimeMillis() + new Random().nextInt(10000);
        orderInfo.setOutTradeNo(outTradeNo);
        //  订单的描述信息:将商品的名称定义为订单的描述信息
        orderInfo.setTradeBody("购买国产手机嘎嘎香");
        orderInfo.setOperateTime(new Date());
        //  过期时间：所有的商品默认为24小时
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, 1);
        orderInfo.setExpireTime(calendar.getTime());
        //  进度状态：
        orderInfo.setProcessStatus(ProcessStatus.UNPAID.name());
        orderInfoMapper.insert(orderInfo);
        //  获取到订单Id
        Long orderId = orderInfo.getId();
        //  order_detail
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        if (!CollectionUtils.isEmpty(orderDetailList)) {
            orderDetailList.forEach(orderDetail -> {
                //  细节：
                orderDetail.setOrderId(orderId);
                orderDetailMapper.insert(orderDetail);
            });
        }
        //  返回订单Id
        return orderId;
    }

    @Override
    public String getTradeNo(String userId) {
        //  key 不能重复
        String key = "tradeNo:"+userId;
        //  声明一个变量接收流水号
        String tradeNo = UUID.randomUUID().toString();
        //  存储到redis 中
        this.redisTemplate.opsForValue().set(key,tradeNo);
        //  返回流水号
        return tradeNo;
    }

    @Override
    public Boolean checkTradeNo(String tradeNo,String userId) {
        //  key 不能重复
        String key = "tradeNo:"+userId;
        //  获取缓存流水号判断
        String redisTradeNo = (String) this.redisTemplate.opsForValue().get(key);
        //  返回比较结果
        return tradeNo.equals(redisTradeNo);
    }

    @Override
    public void delTradeNo(String userId) {
        //  key 不能重复
        String key = "tradeNo:"+userId;
        this.redisTemplate.delete(key);
    }

    @Override
    public Boolean checkStock(Long skuId, Integer skuNum) {
        //  远程调用库存系统接口: http://localhost:9001/hasStock?skuId=10221&num=2
        // http://localhost:9001
        String res = HttpClientUtil.doGet(wareUrl + "/hasStock?skuId=" + skuId + "&num=" + skuNum);
        //  返回比较结果。
        return "1".equals(res);
    }
}
