package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.SpuInfo;
import com.atguigu.gmall.product.service.ManagerService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.api.R;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController //组合注解 @ResponseBody @Controller  @ResponseBody：a.返回json数据  b.能将数据直接显示到页面
@RequestMapping("admin/product/")//表示在当前这个类中，有很多映射路径，所有的映射路径都是以 admin/product/
public class SpuManageController {

    @Autowired
    private ManagerService managerService;
    // springmvc 获取数据：
    //      1.@RequestParam Long category3Id,
    //      2.对象传值：参数名称与实体类的属性名一致，则会自动映射
    ///admin/product/{page}/{limit}
    /**
     * 根据三级分类Id 查询spu 列表！
     * @param page 第几页
     * @param limit 每页显示的条数
     * @return
     */
    @GetMapping("{page}/{limit}")
    public Result getSpuList(@PathVariable Long page,
                             @PathVariable Long limit,
                             SpuInfo spuInfo
                             ){
        //mybatis-puls 提供了一个分页对象
        Page<SpuInfo> spuInfoPage = new Page<>(page,limit);
        // 调用服务层方法，封装分页数据
        IPage<SpuInfo> infoIPage = this.managerService.getSpuList(spuInfoPage,spuInfo);
        // 返回数据
        return Result.ok(infoIPage);
    }
}
