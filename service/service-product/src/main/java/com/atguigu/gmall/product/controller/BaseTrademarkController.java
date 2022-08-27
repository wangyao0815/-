package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.BaseTrademark;
import com.atguigu.gmall.model.product.SpuInfo;
import com.atguigu.gmall.product.service.BaseTrademarkService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController //组合注解 @ResponseBody @Controller  @ResponseBody：a.返回json数据  b.能将数据直接显示到页面
@RequestMapping("admin/product/baseTrademark/")//表示在当前这个类中，有很多映射路径，所有的映射路径都是以 admin/product/
public class BaseTrademarkController {

    @Autowired
    private BaseTrademarkService baseTrademarkService;

    //品牌分类列表
    ///admin/product/baseTrademark/{page}/{limit}
    @GetMapping("{page}/{limit}")
    public Result getTradeMarkList(@PathVariable Long page,
                             @PathVariable Long limit
                            ){
        //构建Page
        Page<BaseTrademark> baseTrademarkPage = new Page<>(page,limit);
        //调用服务层方法
        IPage iPage = this.baseTrademarkService.getTradeMarkList(baseTrademarkPage);
        //返回数据
        return Result.ok(iPage);
    }
}
