package com.atguigu.gmall.order.controller;

import com.atguigu.gmall.cart.client.CartFeignClient;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.util.AuthContextHolder;
import com.atguigu.gmall.model.cart.CartInfo;
import com.atguigu.gmall.model.order.OrderDetail;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.user.UserAddress;
import com.atguigu.gmall.order.service.OrderService;
import com.atguigu.gmall.product.client.ProductFeignClient;
import com.atguigu.gmall.user.client.UserFeignClient;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
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

    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private RedisTemplate redisTemplate;

    //  /api/order/auth/trade
    //  订单结算页面原创调用url
    @GetMapping("/auth/trade")
    public Result authTrade(HttpServletRequest request) {
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
        hashMap.put("userAddressList", userAddressList);
        hashMap.put("detailArrayList", detailArrayList);
        hashMap.put("totalNum", totalNum);// 总件数
        hashMap.put("totalAmount", orderInfo.getTotalAmount());// 总价格
        hashMap.put("tradeNo", this.orderService.getTradeNo(userId));// 总价格
        //  返回数据
        return Result.ok(hashMap);
    }

    //  保存订单  http://api.gmall.com/api/order/auth/submitOrder?tradeNo=34567rtyui
    //  auth/submitOrder
    @PostMapping("auth/submitOrder")
    public Result submitOrder(@RequestBody OrderInfo orderInfo, HttpServletRequest request) {
        //  获取用户Id
        String userId = AuthContextHolder.getUserId(request);
        orderInfo.setUserId(Long.parseLong(userId));

        //  获取到页面传递的流水号：
        String tradeNo = request.getParameter("tradeNo");
        //  调用比较方法
        Boolean result = this.orderService.checkTradeNo(tradeNo, userId);
        //  表示比较失败
        if (!result) {
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
        //  2个订单明细：23,24
        //  声明一个多线程的集合
        ArrayList<CompletableFuture> completableFutureArrayList = new ArrayList<>();
        //  声明一个String 数据类型的集合来存储 信息提示
        ArrayList<String> errorList = new ArrayList<>();

        for (OrderDetail orderDetail : orderDetailList) {
            //  获取skuId,skuNum
            Long skuId = orderDetail.getSkuId();
            Integer skuNum = orderDetail.getSkuNum();
            //  校验库存
            CompletableFuture<Void> stockCompletableFuture = CompletableFuture.runAsync(() -> {
                //  调用校验库存系统接口
                Boolean exist = this.orderService.checkStock(skuId, skuNum);
                if (!exist) {
                    errorList.add(orderDetail.getSkuId() + "库存不足！");
                }
            });
            //  添加到集合中
            completableFutureArrayList.add(stockCompletableFuture);

            CompletableFuture<Void> priceCompletableFuture = CompletableFuture.runAsync(() -> {
                //  检验价格: 订单价格 与 商品的实时价格  1999
                BigDecimal orderPrice = orderDetail.getOrderPrice();
                //  商品的实时价格： 2000
                BigDecimal skuPrice = this.productFeignClient.getSkuPrice(skuId);
                //  价格比较：
                //  return xs ! = ys ? ((xs > ys) ? 1 : -1) :0 ;
                //  int i = orderPrice.compareTo(skuPrice);
                //  什么时候涨价--什么时候降价
                if (orderPrice.compareTo(skuPrice) != 0) {
                    //  获取信息提示
                    String msg = orderPrice.compareTo(skuPrice) > 0 ? "降价" : "涨价";
                    //  没有变动！ 变动了多少!    变动的价格
                    BigDecimal price = orderPrice.subtract(skuPrice).abs();

                    //  自动更新购物车价格：orderDetail.getSkuId()
                    String cartKey = RedisConst.USER_KEY_PREFIX + userId + RedisConst.USER_CART_KEY_SUFFIX;
                    //  hget key field;
                    CartInfo cartInfo = (CartInfo) this.redisTemplate.opsForHash().get(cartKey, skuId.toString());
                    cartInfo.setSkuPrice(skuPrice);
                    //  hset key field value
                    this.redisTemplate.opsForHash().put(cartKey, skuId.toString(), cartInfo);
                    //  返回消息提示
                    errorList.add(orderDetail.getSkuId() + "价格" + msg + price);
                }
            });

            completableFutureArrayList.add(priceCompletableFuture);
        }
        //  多任务组合：所有的异步编排对象都在集合中
        //  int arrays [] = new int[3];
        CompletableFuture.allOf(completableFutureArrayList.toArray(new CompletableFuture[completableFutureArrayList.size()])).join();
        //  判断
        if (errorList.size() > 0) {
            //  将集合中的数据使用，进行拼接成字符串
            return Result.fail().message(StringUtils.join(errorList, ","));
        }
        //  调用服务层方法
        Long orderId = this.orderService.saveOrderInfo(orderInfo);
        //  返回订单Id
        return Result.ok(orderId);
    }

    //  查看我的订单
    @GetMapping("/auth/{page}/{limit}")
    public Result getMyOrderList(@PathVariable Long page,
                                 @PathVariable Long limit,
                                 HttpServletRequest request){
        //  获取用户Id
        String userId = AuthContextHolder.getUserId(request);
        //  封装page对象
        Page<OrderInfo> orderInfoPage = new Page<>(page,limit);
        //  调用服务层方法
        IPage<OrderInfo> orderInfoIPage = this.orderService.getMyOrderList(orderInfoPage,userId);
        //  返回数据
        return Result.ok(orderInfoIPage);
    }

    //  根据订单Id 获取订单信息
    @GetMapping("inner/getOrderInfo/{orderId}")
    public OrderInfo getOrderInfo(@PathVariable Long orderId){
        //  只有irderInfo ，没有订单明细  ----  后续拆单要用订单明细
        //  OrderInfo orderInfo = this.orderService.getById(orderId);
        OrderInfo orderInfo = this.orderService.getOrderInfo(orderId);
        return orderInfo;
    }

}
