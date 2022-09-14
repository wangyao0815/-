package com.atguigu.gmall.user.service;

import com.atguigu.gmall.model.user.UserAddress;

import java.util.List;

public interface UserAddressService {
    //  获取用户的收货地址列表
    List<UserAddress> getUserAddressListByUserId(String userId);
}
