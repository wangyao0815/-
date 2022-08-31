package com.atguigu.gmall.item.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.item.service.ItemService;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ItemServiceImpl implements ItemService {

    @Autowired
    private ProductFeignClient productFeignClient;

    @Override
    public Map<String, Object> getItem(Long skuId) {
        //声明map 集合
        Map<String, Object> map = new HashMap<>();
        //获取商品的基本信息 + 商品的图片列表
        SkuInfo skuInfo = this.productFeignClient.getSkuInfo(skuId);
        //获取分类数据
        BaseCategoryView categoryView = this.productFeignClient.getCategoryView(skuInfo.getCategory3Id());
        //获取价格
        BigDecimal skuPrice = this.productFeignClient.getSkuPrice(skuId);
        //  获取销售属性+属性值+锁定
        List<SpuSaleAttr> spuSaleAttrList = this.productFeignClient.getSpuSaleAttrListCheckBySku(skuId, skuInfo.getSpuId());
        //  获取海报
        List<SpuPoster> spuPosterList = this.productFeignClient.getSpuPosterBySpuId(skuInfo.getSpuId());
        //  获取json 字符串
        Map skuValueIdsMap = this.productFeignClient.getSkuValueIdsMap(skuInfo.getSpuId());
        // map 转Json
        String strJson = JSON.toJSONString(skuValueIdsMap);
        //  获取商品规格参数--平台属性
        List<BaseAttrInfo> attrList = this.productFeignClient.getAttrList(skuId);
        // attrName  attrValue
        if (!CollectionUtils.isEmpty(attrList)){
            List<HashMap<String, Object>> attrMapList = attrList.stream().map(baseAttrInfo -> {
                //  为了迎合页面数据存储，定义一个map集合
                HashMap<String, Object> hashMap = new HashMap<>();
                //  将map 看做一个JAVA对象
                hashMap.put("attrName", baseAttrInfo.getAttrName());
                hashMap.put("attrValue", baseAttrInfo.getAttrValueList().get(0).getValueName());
                return hashMap;
            }).collect(Collectors.toList());
            //  保存规格参数：平台属性名称： 平台属性值名称
            map.put("skuAttrList", attrMapList);
        }
        // key是谁？应该是页面渲染时需要的key
        map.put("skuInfo", skuInfo);
        map.put("categoryView", categoryView);
        map.put("price", skuPrice);
        map.put("spuSaleAttrList", spuSaleAttrList);
        map.put("spuPosterList", spuPosterList);
        map.put("valuesSkuJson", strJson);

        // 返回数据
        return map;
    }
}
