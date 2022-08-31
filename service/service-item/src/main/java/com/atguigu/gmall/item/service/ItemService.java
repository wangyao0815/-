package com.atguigu.gmall.item.service;

import java.util.Map;

public interface ItemService {
    /**
     * 根据 spuId 获取渲染数据
     * @param skuId
     * @return
     */
    Map<String, Object> getItem(Long skuId);
}
