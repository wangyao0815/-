package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.product.service.ManageService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController //组合注解 @ResponseBody @Controller  @ResponseBody：a.返回json数据  b.能将数据直接显示到页面
@RequestMapping("admin/product")//表示在当前这个类中，有很多映射路径，所有的映射路径都是以 admin/product/
public class SkuManageController {

    @Autowired
    private ManageService manageService;

    // http://localhost/admin/product/saveSkuInfo
    @PostMapping("saveSkuInfo")
    public Result saveSkuInfo(@RequestBody SkuInfo skuInfo){
        // 调用服务层方法
        this.manageService.saveSkuInfo(skuInfo);
        return Result.ok();
    }

    // http://localhost/admin/product/list/1/10?category3Id=61
    //根据三级分类Id，查询skuInfo 列表
    @GetMapping("/list/{page}/{limit}")
    public Result getskuInfoList(@PathVariable Long page,
                                 @PathVariable Long limit,
                                 SkuInfo skuInfo){
        //创建一个分页对象
       Page<SkuInfo> skuInfoPage = new Page<>(page, limit);
        //调用服务层
        IPage iPage = this.manageService.getskuInfoList(skuInfoPage,skuInfo);
        //返回数据
        return Result.ok(iPage);
    }

    // /admin/product/onSale/{skuId}
    //http://localhost/admin/product/onSale/23
    //上架：is_sale = 1
    @GetMapping("onSale/{skuId}")
    public Result onSale(@PathVariable Long skuId){
        //调用服务层
        this.manageService.onSale(skuId);
        return Result.ok();
    }

    //http://localhost/admin/product/cancelSale/20
    // /admin/product/cancelSale/{skuId}
    // 下架：is_sale = 0
    @GetMapping("cancelSale/{skuId}")
    public Result cancelSale(@PathVariable Long skuId){
        //调用服务层
        this.manageService.cancelSale(skuId);
        return Result.ok();
    }
}
