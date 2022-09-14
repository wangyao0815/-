package com.atguigu.gmall.cart.service.impl;

import com.atguigu.gmall.cart.service.CartService;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.util.DateUtil;
import com.atguigu.gmall.model.cart.CartInfo;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private ProductFeignClient productFeignClient;

    @Override
    public void addToCart(Long skuId, String userId, Integer skuNum) {
        /*
        1.  判断购物车中是否有该商品 ，
            如果有 则数量相加
            如果没有 则直接添加商品,小于等于200
            显示实时价格

        2.  每次添加购物车的时候，这个商品都应该是默认选中状态！

        3.  按照修改时间排序！

        4.  保存数据 -- redis
         */
        //  数据类型Hash    谁的购物车  key = user:userId:cart field = skuId.toString()  value = CartInfo;
        String carKey = getCartKey(userId);
        //  hget key field;
        CartInfo cartInfoExist = (CartInfo) this.redisTemplate.opsForHash().get(carKey, skuId.toString());
        //  判断是否存在
        if (cartInfoExist != null) {
            //  有这个购物项  保存商品数量不能超过200
            if (cartInfoExist.getSkuNum() + skuNum > 200) {
                cartInfoExist.setSkuNum(200);
            } else {
                cartInfoExist.setSkuNum(cartInfoExist.getSkuNum() + skuNum);
            }
            //  Integer num = cartInfoExist.getSkuNum()+skuNum>200?200:cartInfoExist.getSkuNum()+skuNum;
            //  cartInfoExist.setSkuNum(cartInfoExist.getSkuNum()+skuNum>200?200:cartInfoExist.getSkuNum()+skuNum);
            //  显示实时价格
            cartInfoExist.setSkuPrice(productFeignClient.getSkuPrice(skuId));
            //  设置选中状态
            if (cartInfoExist.getIsChecked().intValue() == 0) {
                cartInfoExist.setIsChecked(1);
            }

            //  设置修改时间
            cartInfoExist.setUpdateTime(new Date());

            //  存储到缓存
            //  this.redisTemplate.opsForHash().put(carKey, skuId.toString(), cartInfoExist);
        } else {
            //  当前这个商品不存在
            cartInfoExist = new CartInfo();
            //  根据skuId 来获取skuInfo
            SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
            cartInfoExist.setSkuId(skuId);
            cartInfoExist.setSkuNum(skuNum);
            cartInfoExist.setUserId(userId);
            //  实时价格
            cartInfoExist.setSkuPrice(productFeignClient.getSkuPrice(skuId));
            //  加入购物车时的价格
            cartInfoExist.setCartPrice(productFeignClient.getSkuPrice(skuId));
            cartInfoExist.setSkuName(skuInfo.getSkuName());
            cartInfoExist.setImgUrl(skuInfo.getSkuDefaultImg());
            cartInfoExist.setCreateTime(new Date());
            cartInfoExist.setUpdateTime(new Date());
        }
        //  存储到缓存
        this.redisTemplate.opsForHash().put(carKey, skuId.toString(), cartInfoExist);
    }

    @Override
    public List<CartInfo> getCartList(String userId, String userTempId) {

        //  声明一个集合列表
        List<CartInfo> cartInfoNoLoginList = new ArrayList<>();
        /*
        demo:
            登录：
                17  1
                18  1

            未登录：
                17  1
                18  1
                19  2

             合并：
                17  2
                18  2
                19  2
         */
        //  判断临时用户Id 不为空！ userId 为空！
        if (!StringUtils.isEmpty(userTempId)){
            //  获取缓存的key
            String cartKey = this.getCartKey(userTempId);
            //  hget key field; hvals key
            cartInfoNoLoginList = this.redisTemplate.opsForHash().values(cartKey);
            if (StringUtils.isEmpty(userId)){
                //  排序
                if (!CollectionUtils.isEmpty(cartInfoNoLoginList)){
                    //  排序：查看的时候，按照更新时间排序降序！
                    cartInfoNoLoginList.sort((o1,o2)->{
                        return DateUtil.truncatedCompareTo(o2.getUpdateTime(),o1.getUpdateTime(), Calendar.SECOND);
                    });
                }
                //  返回未登录购物车集合数据！
                return cartInfoNoLoginList;
            }
        }


        //  声明一个登录购物车集合
        List<CartInfo> cartInfoLoginList = new ArrayList<>();
        //  判断获取到用户登录的购物车数据
        if (!StringUtils.isEmpty(userId)){
            //  获取缓存的key
            String cartKey = this.getCartKey(userId);
            //  获取数据
            BoundHashOperations<String, String, CartInfo> boundHashOperations = this.redisTemplate.boundHashOps(cartKey);
            //  hget key field; = boundHashOperations.get()
            //  hvals key = List<CartInfo> values = boundHashOperations.values();
            if (!CollectionUtils.isEmpty(cartInfoNoLoginList)){
                //  合并！
                cartInfoNoLoginList.forEach(cartInfoNoLogin -> {
                    //  cartInfoNoLogin.getSkuId().compareTo(cartInfo.getSkuId())==0  17，18
                    if (boundHashOperations.hasKey(cartInfoNoLogin.getSkuId().toString())){
                        //  skuNum 相加
                        CartInfo cartInfoLogin = boundHashOperations.get(cartInfoNoLogin.getSkuId().toString());

                        if (cartInfoLogin.getSkuNum()+cartInfoNoLogin.getSkuNum()>200){
                            cartInfoLogin.setSkuNum(200);
                        }else {
                            cartInfoLogin.setSkuNum(cartInfoLogin.getSkuNum()+cartInfoNoLogin.getSkuNum());
                        }

                        //  选中状态: 未登录状态
                        if (cartInfoNoLogin.getIsChecked().intValue()==1){
                            //  登录状态
                            if (cartInfoLogin.getIsChecked().intValue()==0){
                                cartInfoLogin.setIsChecked(1);
                            }
                        }

                        //  相当于修改了
                        cartInfoLogin.setUpdateTime(new Date());

                        //  写回缓存
                        this.redisTemplate.boundHashOps(cartKey).put(cartInfoLogin.getSkuId().toString(), cartInfoLogin);
                        //  boundHashOperations.put(cartInfoLogin.getSkuId().toString(), cartInfoLogin);
                    }else {
                        //  未找到相同的数据  19
                        if (cartInfoNoLogin.getIsChecked().intValue()==1){
                            cartInfoNoLogin.setUserId(userId);
                            cartInfoNoLogin.setCreateTime(new Date());
                            cartInfoNoLogin.setUpdateTime(new Date());
                            this.redisTemplate.opsForHash().put(cartKey, cartInfoNoLogin.getSkuId().toString(),cartInfoNoLogin);
                        }
                    }
                });
                //  删除未登录购物车
                this.redisTemplate.delete(this.getCartKey(userTempId));
            }
            //  查询所有
            cartInfoLoginList = boundHashOperations.values();
        }
        //  如果购物车数据为空，则返回
        if (CollectionUtils.isEmpty(cartInfoLoginList)){
            return new ArrayList<>();
        }
        //  排序：查看的时候，按照更新时间排序降序！
        cartInfoLoginList.sort((o1, o2) -> {
            return DateUtil.truncatedCompareTo(o2.getUpdateTime(), o1.getUpdateTime(), Calendar.SECOND);
        });
        //  返回数据
        return cartInfoLoginList;

        //        //  创建一个登录购物车集合
        //        List<CartInfo> cartInfoLoginList = new ArrayList<>();
        //
        //        //  只能通过一个Id查询，不考虑同时存在的情况
        //        if (!StringUtils.isEmpty(userId)) {
        //            //  说明登录
        //            //  获取缓存的key
        //            String cartKey = this.getCartKey(userId);
        //
        //            //  hget key fueld; hvals key
        //            cartInfoLoginList = this.redisTemplate.opsForHash().values(cartKey);
        //
        //            if (!StringUtils.isEmpty(userTempId)){
        //                //  合并购物车
        //                List<CartInfo> finalCartInfoLoginList = cartInfoLoginList;
        //                cartInfoNoLoginList.forEach(cartInfoNoLogin -> {
        //                    finalCartInfoLoginList.forEach(cartInfo -> {
        //                        //  17  18
        //                        if (cartInfoNoLogin.getSkuId().compareTo(cartInfo.getSkuId())==0){
        //                            //  数量相加
        //                            cartInfo.setSkuNum(cartInfo.getSkuNum()+cartInfoNoLogin.getSkuNum());
        //                            //  同步到缓存
        //                            this.redisTemplate.opsForHash().put(cartKey, cartInfoNoLogin.getSkuId().toString(),cartInfo);
        //                        }else {
        //                            //  直接加入缓存  19
        //                            cartInfoNoLogin.setUserId(userId);
        //                            cartInfoNoLogin.setCreateTime(new Date());
        //                            cartInfoNoLogin.setUpdateTime(new Date());
        //                            this.redisTemplate.opsForHash().put(cartKey, cartInfoNoLogin.getSkuId().toString(),cartInfoNoLogin);
        //                        }
        //                    });
        //                });
        //                //  删除临时购物车数据
        //                this.redisTemplate.delete(getCartKey(userTempId));
        //                //  查询合并之后的数据
        //                cartInfoLoginList = this.redisTemplate.opsForHash().values(cartKey);
        //                //  排序：查看的时候，按照更新时间排序降序！
        //                cartInfoLoginList.sort((o1, o2) -> {
        //                    return DateUtil.truncatedCompareTo(o2.getUpdateTime(), o1.getUpdateTime(), Calendar.SECOND);
        //                });
        //                //  返回数据
        //                return cartInfoLoginList;
        //            }else {
        //                //  排序：查看的时候，按照更新时间排序降序！
        //                cartInfoLoginList.sort((o1, o2) -> {
        //                    return DateUtil.truncatedCompareTo(o2.getUpdateTime(), o1.getUpdateTime(), Calendar.SECOND);
        //                });
        //                //  返回数据
        //                return cartInfoLoginList;
        //            }
        //        }
        //  返回数据
        //  return null;
    }

    @Override
    public void CheckCart(Long skuId, String userId, Integer isChecked) {
        //  hget key field
        String cartKey = this.getCartKey(userId);
        //  获取数据
        CartInfo cartInfo = (CartInfo) this.redisTemplate.opsForHash().get(cartKey, skuId.toString());
        if (cartInfo!=null){
            //  赋值并写回缓存
            cartInfo.setIsChecked(isChecked);
            //  cartInfo.setUpdateTime(new Date()); 灵活选项
            this.redisTemplate.opsForHash().put(cartKey, skuId.toString(), cartInfo);
        }
    }

    @Override
    public void deleteCart(Long skuId, String userId) {
        //  获取key
        String cartKey = this.getCartKey(userId);
        //  hdel key field
        this.redisTemplate.opsForHash().delete(cartKey, skuId.toString());
    }

    @Override
    public List<CartInfo> getCartCheckedList(String userId) {
        //  创建一个购物车集合
        //  ArrayList<CartInfo> cartInfos = new ArrayList<>();
        //  获取购物车数据
        String cartKey = this.getCartKey(userId);
        List<CartInfo> cartInfoList = this.redisTemplate.opsForHash().values(cartKey);

        //  根据用户Id 来获取选中状态的购物项 is_checked = 1
        //        cartInfoList.forEach(cartInfo -> {
        //            if (cartInfo.getIsChecked().intValue()==1) {
        //                cartInfos.add(cartInfo);
        //            }
        //        });
        List<CartInfo> cartInfoCheckedList = cartInfoList.stream().filter(cartInfo -> {
            return cartInfo.getIsChecked().intValue() == 1;
        }).collect(Collectors.toList());
        //  返回购物车集合
        return cartInfoCheckedList;
    }

    private String getCartKey(String userId) {
        String carKey = RedisConst.USER_KEY_PREFIX + userId + RedisConst.USER_CART_KEY_SUFFIX;
        return carKey;
    }
}
