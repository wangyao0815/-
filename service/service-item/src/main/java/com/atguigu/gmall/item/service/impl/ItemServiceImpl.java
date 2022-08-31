package com.atguigu.gmall.item.service.impl;

import com.atguigu.gmall.item.service.ItemService;
import com.atguigu.gmall.model.product.SkuInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class ItemServiceImpl implements ItemService {

    //    @Autowired
    //    private ProductFeignClient productFeignClient;

    @Override
    public Map<String, Object> getItem(Long skuId) {
        //声明map 集合
        Map<String, Object> map = new HashMap<>();
        //        SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
        //        map.put("skuInfo", skuInfo);
        //        map.put("price", price);
        // 返回数据
        return map;
    }
}
