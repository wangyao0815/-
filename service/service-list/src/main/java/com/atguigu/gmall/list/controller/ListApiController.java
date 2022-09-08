package com.atguigu.gmall.list.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.list.Goods;
import com.atguigu.gmall.list.SearchParam;
import com.atguigu.gmall.list.SearchResponseVo;
import com.atguigu.gmall.list.service.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("api/list")
public class ListApiController {

    @Autowired
    private ElasticsearchRestTemplate restTemplate;

    @Autowired
    private SearchService searchService;
    @GetMapping("createIndex")
    public Result createIndex(){
        //过时
        restTemplate.createIndex(Goods.class);
        restTemplate.putMapping(Goods.class);
        return Result.ok();
    }

    //  设置一个上架 - 下架
    @GetMapping("inner/upperGoods/{skuId}")
    public Result upperGoods(@PathVariable Long skuId){
        //调用上架方法
        this.searchService.upperGoods(skuId);
        return Result.ok();
    }

    //  设置一个上架 - 下架
    @GetMapping("inner/lowerGoods/{skuId}")
    public Result lowerGoods(@PathVariable Long skuId){
        //调用上架方法
        this.searchService.lowerGoods(skuId);
        return Result.ok();
    }

    //  商品的热度排名
    @GetMapping("inner/incrHotScore/{skuId}")
    public Result incrHotScore(@PathVariable Long skuId){
        //调用服务层
        this.searchService.incrHotScore(skuId);
        return Result.ok();
    }

    //  检索控制器
    @PostMapping
    public Result search(@RequestBody SearchParam searchParam){
        //调用服务层方法
        SearchResponseVo responseVo = null;
        try {
            responseVo = this.searchService.search(searchParam);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //  返回数据
        return Result.ok(responseVo);
    }
}
