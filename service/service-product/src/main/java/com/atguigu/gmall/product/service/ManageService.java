package com.atguigu.gmall.product.service;

import com.atguigu.gmall.model.product.*;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface ManageService {
    /**
     * 查询所有一级分类数据
     * @return
     */
    List<BaseCategory1> getCategory1();

    /**
     * 根据一级分类Id,查询二级分类数据
     * @param category1Id
     * @return
     */
    List<BaseCategory2> getCategory2(Long category1Id);

    /**
     * 根据二级分类Id,查询三级分类数据
     * @param category2Id
     * @return
     */
    List<BaseCategory3> getCategory3(Long category2Id);

    /**
     * 根据分类Id 查询平台属性
     * @param category1Id
     * @param category2Id
     * @param category3Id
     * @return
     */
    List<BaseAttrInfo> getAttrInfoList(Long category1Id, Long category2Id, Long category3Id);

    /**
     * 保存平台属性
     * @param baseAttrInfo
     */
    void saveAttrInfo(BaseAttrInfo baseAttrInfo);

    /**
     * 根据平台属性id 查询平台属性集合
     * @param attrId
     * @return
     */
    List<BaseAttrValue> getAttrValueList(Long attrId);

    /**
     * 根据平台属性Id，查询平台属性数据
     * @param attrId
     * @return
     */
    BaseAttrInfo getAttrInfo(Long attrId);

    /**
     * 根据三级分类Id 查询spu 列表
     * @param spuInfoPage
     * @param spuInfo
     * @return
     */
    IPage<SpuInfo> getSpuList(Page<SpuInfo> spuInfoPage, SpuInfo spuInfo);

    /**
     * 查询所有销售属性数据
     * @return
     */
    List<BaseSaleAttr> getBaseSaleAttrList();

    /**
     * 保存spuInfo
     * @param spuInfo
     */
    void saveSpuInfo(SpuInfo spuInfo);

    /**
     * 根据spuId 获取spuImage 集合
     * @param spuId
     * @return
     */
    List<SpuImage> getSpuImageList(Long spuId);

    /**
     * 根据spuId 获取销售属性集合列表
     * @param spuId
     * @return
     */
    List<SpuSaleAttr> getSpuSaleAttrList(Long spuId);

    /**
     * 保存skuInfo
     * @param skuInfo
     */
    void saveSkuInfo(SkuInfo skuInfo);

    /**
     * 根据三级分类Id获取到skuInfo 列表
     * @param skuInfoPage
     * @param skuInfo
     * @return
     */
    IPage getskuInfoList(Page<SkuInfo> skuInfoPage, SkuInfo skuInfo);

    /**
     *上架
     * @param skuId
     */
    void onSale(Long skuId);

    /**
     * 下架
     * @param skuId
     */
    void cancelSale(Long skuId);

    /**
     * 根据 skuId 获取商品基本数据
     * @param skuId
     * @return
     */
    SkuInfo getSkuInfo(Long skuId);

    /**
     * 根据三级分类Id 获取分类数据
     * @param category3Id
     * @return
     */
    BaseCategoryView getBaseCategoryView(Long category3Id);

    /**
     * 根据skuId 查询价格
     * @param skuId
     * @return
     */
    BigDecimal getSkuPrice(Long skuId);

    /**
     * 据skuId 与 spuId 获取销售属性数据
     * @param skuId
     * @param spuId
     * @return
     */
    List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(Long skuId, Long spuId);

    /**
     * 根据spuId 查询map 集合属性 获取数据
     * @param spuId
     * @return
     */
    Map getSkuValueIdsMap(Long spuId);

    /**
     *获取数据
     * @param spuId
     * @return
     */
    List<SpuPoster> findSpuPosterBySpuId(Long spuId);

    /**
     * 据skuId 获取到规格参数
     * @param skuId
     * @return
     */
    List<BaseAttrInfo> getAttrList(Long skuId);
}
