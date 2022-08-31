package com.atguigu.gmall.product.api;

import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.service.ManageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/product")
public class ProductApiController {

    @Autowired
    private ManageService manageService;

    // 定义根据skuInfo + skuImage 集合数据！
    // /api/product/inner/getSkuInfo/{skuId}  -- inner 内部数据接口 这个接口表面只能给内部的微服务模块使用
    @GetMapping("inner/getSkuInfo/{skuId}")
    public SkuInfo getSkuInfo(@PathVariable Long skuId){
        //调用服务层方法
        return this.manageService.getSkuInfo(skuId);
    }

    //  根据三级分类Id 获取 分类数据
    //  为什么将category3Id 当做了已知条件直接传入了？
    @GetMapping("inner/getCategoryView/{category3Id}")
    public BaseCategoryView getBaseCategoryView(@PathVariable Long category3Id){
        //调用服务层方法
        return this.manageService.getBaseCategoryView(category3Id);
    }

    //  获取最新价格：
    @GetMapping("inner/getSkuPrice/{skuId}")
    public BigDecimal getSkuPrice(@PathVariable Long skuId){
        return this.manageService.getSkuPrice(skuId);
    }

    //  根据skuId 与 spuId 获取销售属性数据
    @GetMapping("inner/getSpuSaleAttrListCheckBySku/{skuId}/{spuId}")
    public List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(@PathVariable Long skuId,
                                                          @PathVariable Long spuId){
        //返回数据
        return this.manageService.getSpuSaleAttrListCheckBySku(skuId,spuId);
    }

    //根据spuId 查询map 集合属性 获取数据
    @GetMapping("inner/getSkuValueIdsMap/{spuId}")
    public Map getSkuValueIdsMap(@PathVariable Long spuId){
        return this.manageService.getSkuValueIdsMap(spuId);
    }

    // /api/product/inner/findSpuPosterBySpuId/{spuId}
    @GetMapping("inner/findSpuPosterBySpuId/{spuId}")
    public List<SpuPoster> findSpuPosterBySpuId(@PathVariable Long spuId){
        //获取数据
        return this.manageService.findSpuPosterBySpuId(spuId);
    }

    //根据skuId 获取到规格参数
    @GetMapping("inner/getAttrList/{skuId}")
    public List<BaseAttrInfo> getAttrList(@PathVariable Long skuId){
        return this.manageService.getAttrList(skuId);
    }
}
