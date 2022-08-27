package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.BaseAttrInfo;
import com.atguigu.gmall.model.product.BaseCategory1;
import com.atguigu.gmall.model.product.BaseCategory2;
import com.atguigu.gmall.model.product.BaseCategory3;
import com.atguigu.gmall.product.service.ManagerService;
import org.apache.catalina.manager.ManagerServlet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController //组合注解 @ResponseBody @Controller  @ResponseBody：a.返回json数据  b.能将数据直接显示到页面
@RequestMapping("admin/product/")//表示在当前这个类中，有很多映射路径，所有的映射路径都是以 admin/product/
public class ManageController {

    @Autowired
    private ManagerService managerService;

    //http://localhost/admin/product/getCategory1
    @GetMapping("getCategory1")
    public Result getCategory1(){
        //调用服务层  select * from base_category1 where is_deleted = 0;
        List<BaseCategory1> baseCategory1List = managerService.getCategory1();
        //返回数据
        return Result.ok(baseCategory1List);
    }

    // 根据一级分类Id 查询二级分类数据
    // url 路径从哪来？http://localhost/admin/product/getCategory2/{category1Id}
    // springmvc restful 参数的数据类型如何判断？
    @GetMapping("getCategory2/{category1Id}")
    public Result getCategory2(@PathVariable Long category1Id){
        // 调用服务层方法
        List<BaseCategory2> baseCategory2List = this.managerService.getCategory2(category1Id);
        return Result.ok(baseCategory2List);
    }

    // 根据二级分类Id 查询三级分类数据
    @GetMapping("getCategory3/{category2Id}")
    public Result getCategory3(@PathVariable Long category2Id){
        // 调用服务层方法
        List<BaseCategory3> baseCategory3List = this.managerService.getCategory3(category2Id);
        return Result.ok(baseCategory3List);
    }
    //根据分类Id 查询平台属性数据
    @GetMapping("attrInfoList/{category1Id}/{category2Id}/{category3Id}")
    public Result getAttrInfoList(@PathVariable Long category1Id,
                                  @PathVariable Long category2Id,
                                  @PathVariable Long category3Id){
        // 调用服务层方法:泛型
        List<BaseAttrInfo> baseAttrInfoList= this.managerService.getAttrInfoList(category1Id,category2Id,category3Id);
        // 返回数据；
        return Result.ok(baseAttrInfoList);
    }
    //保存平台属性
    @PostMapping("saveAttrInfo")
    public Result saveAttrInfo(@RequestBody BaseAttrInfo baseAttrInfo){
        //调用服务层方法
         this.managerService.saveAttrInfo(baseAttrInfo);
         //返回数据
        return Result.ok();
    }
}
