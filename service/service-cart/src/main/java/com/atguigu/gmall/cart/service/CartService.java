package com.atguigu.gmall.cart.service;

import com.atguigu.gmall.model.cart.CartInfo;

import java.util.List;

public interface CartService {
    // 添加购物车 用户Id，商品Id，商品数量。
    void addToCart(Long skuId, String userId, Integer skuNum);
    //  查看购物车列表
    List<CartInfo> getCartList(String userId, String userTempId);
    //  修改选中状态
    void CheckCart(Long skuId, String userId, Integer isChecked);
    //  删除购物车
    void deleteCart(Long skuId,String userId);
    //  根据用户Id获取选中状态的购物项
    List<CartInfo> getCartCheckedList(String userId);
}
