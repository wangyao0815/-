package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.BaseCategoryTrademark;
import com.atguigu.gmall.model.product.BaseTrademark;
import com.atguigu.gmall.model.product.CategoryTrademarkVo;
import com.atguigu.gmall.product.service.BaseCategoryTrademarkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController //组合注解 @ResponseBody @Controller  @ResponseBody：a.返回json数据  b.能将数据直接显示到页面
@RequestMapping("/admin/product/baseCategoryTrademark/")//表示在当前这个类中，有很多映射路径，所有的映射路径都是以 admin/product/
public class BaseCategoryTrademarkController {

    @Autowired
    private BaseCategoryTrademarkService baseCategoryTrademarkService;

    //  /admin/product/baseCategoryTrademark/findTrademarkList/{category3Id}
    @GetMapping("findTrademarkList/{category3Id}")
    public Result findTrademarkList(@PathVariable Long category3Id){
        //调用服务层
        List<BaseTrademark> baseTrademarkList = this.baseCategoryTrademarkService.findTrademarkList(category3Id);
        //返回数据
        return Result.ok(baseTrademarkList);
    }


    //获取可选品牌列表
    // http://localhost/admin/product/baseCategoryTrademark/findCurrentTrademarkList/{category3Id}
    @GetMapping("findCurrentTrademarkList/{category3Id}")
    public Result findCurrentTrademarkList(@PathVariable Long category3Id){
        //调用服务处层
        List<BaseTrademark> baseTrademarkList = this.baseCategoryTrademarkService.findCurrentTrademarkList(category3Id);
        //返回数据
        return Result.ok(baseTrademarkList);
    }

    //保存分类Id 与品牌的关系
    // http://localhost/admin/product/baseCategoryTrademark/save
    @PostMapping("save")
    public Result save(@RequestBody CategoryTrademarkVo categoryTrademarkVo){
        //调用服务层方法
        this.baseCategoryTrademarkService.save(categoryTrademarkVo);
        // 默认返回数据
        return Result.ok();
    }

    //删除品牌与分类的关系、
    // http://localhost/admin/product/baseCategoryTrademark/remove/61/1
    @DeleteMapping("remove/{category3Id}/{tmId}")
    public Result remove(@PathVariable Long category3Id,
                         @PathVariable Long tmId){
        //调用服务层方法
        this.baseCategoryTrademarkService.removeByCategory3IdAndTmId(category3Id,tmId);
        // 默认返回数据
        return Result.ok();
    }
}
