package com.atguigu.gmall.item.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.client.ListFeignClient;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.item.service.ItemService;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

@Service
public class ItemServiceImpl implements ItemService {

    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

    @Autowired
    private ListFeignClient listFeignClient;

    @Override
    public Map<String, Object> getItem(Long skuId) {
        //声明map 集合
        Map<String, Object> map = new HashMap<>();
        //判断  布隆过滤器
        //        RBloomFilter<Object> bloomFilter = redissonClient.getBloomFilter(RedisConst.SKU_BLOOM_FILTER);
        //        if (!bloomFilter.contains(skuId)) {
        //            return null;
        //        }
        CompletableFuture<SkuInfo> skuInfoCompletableFuture = CompletableFuture.supplyAsync(() -> {
            //获取商品的基本信息 + 商品的图片列表
            SkuInfo skuInfo = this.productFeignClient.getSkuInfo(skuId);
            map.put("skuInfo", skuInfo);
            //  返回对象
            return skuInfo;
        },threadPoolExecutor);

        //获取分类数据  -- 返回给页面使用
        CompletableFuture<Void> categoryViewCompletableFuture = skuInfoCompletableFuture.thenAcceptAsync(skuInfo -> {
            BaseCategoryView categoryView = this.productFeignClient.getCategoryView(skuInfo.getCategory3Id());
            map.put("categoryView", categoryView);
        },threadPoolExecutor);
        //获取价格
        CompletableFuture<Void> priceCompletableFuture = CompletableFuture.runAsync(() -> {
            BigDecimal skuPrice = this.productFeignClient.getSkuPrice(skuId);
            map.put("price", skuPrice);
        },threadPoolExecutor);
        //  获取销售属性+属性值+锁定
        CompletableFuture<Void> spuSaleAttrListCompletableFuture = skuInfoCompletableFuture.thenAcceptAsync(skuInfo -> {
            List<SpuSaleAttr> spuSaleAttrList = this.productFeignClient.getSpuSaleAttrListCheckBySku(skuId, skuInfo.getSpuId());
            map.put("spuSaleAttrList", spuSaleAttrList);
        },threadPoolExecutor);
        //  获取海报
        CompletableFuture<Void> spuPosterListCompletableFuture = skuInfoCompletableFuture.thenAcceptAsync(skuInfo -> {
            List<SpuPoster> spuPosterList = this.productFeignClient.getSpuPosterBySpuId(skuInfo.getSpuId());
            map.put("spuPosterList", spuPosterList);
        },threadPoolExecutor);
        //  获取json 字符串
        CompletableFuture<Void> skuJsonCompletableFuture = skuInfoCompletableFuture.thenAcceptAsync(skuInfo -> {
            Map skuValueIdsMap = this.productFeignClient.getSkuValueIdsMap(skuInfo.getSpuId());
            // map 转Json
            String strJson = JSON.toJSONString(skuValueIdsMap);
            map.put("valuesSkuJson", strJson);
        },threadPoolExecutor);

        //  获取商品规格参数--平台属性

        CompletableFuture<Void> attrListCompletableFuture = CompletableFuture.runAsync(() -> {
            List<BaseAttrInfo> attrList = this.productFeignClient.getAttrList(skuId);
            // attrName  attrValue
            if (!CollectionUtils.isEmpty(attrList)) {
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
        },threadPoolExecutor);

        //  开启一个线程做异步调用热度排名方法
        CompletableFuture<Void> incrCompletableFuture = CompletableFuture.runAsync(() -> {
            listFeignClient.incrHotScore(skuId);
        },threadPoolExecutor);
        //  多任务组合
        CompletableFuture.allOf(
                skuInfoCompletableFuture,
                categoryViewCompletableFuture,
                spuPosterListCompletableFuture,
                priceCompletableFuture,
                spuPosterListCompletableFuture,
                skuJsonCompletableFuture,
                attrListCompletableFuture,
                incrCompletableFuture
                ).join();
        // 返回数据
        return map;
    }
}
