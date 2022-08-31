package com.atguigu.gmall.item.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.item.service.ItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/item")
public class ItemApiController {

    @Autowired
    private ItemService itemService;

    //返回值是谁，如果定义？由web-all决定：后台存储一个 skuInfo 那么，页面就需要 skuInfo.skuName
    // price, skuImageList ...
    @GetMapping("{skuId}")
    public Result getItem(@PathVariable Long skuId){
        //调用服务层方法
        Map<String, Object> map = itemService.getItem(skuId);
        return Result.ok(map);
    }
}
