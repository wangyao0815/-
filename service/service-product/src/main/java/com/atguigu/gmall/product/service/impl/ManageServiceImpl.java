package com.atguigu.gmall.product.service.impl;

import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.mapper.*;
import com.atguigu.gmall.product.service.ManageService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ManageServiceImpl implements ManageService {

    // mapper 层 select * from base_category1 where is_deleted = 0;
    // mapper 重点要执行上述的sql语句，mabatis-plus 执行； 直接调用mapper 就可以了
    // 接口上有 @Mapper 注解，extends BaseMapper<BaseCategory1> BaseMapper 封装好了对单表的crud! 单表指的是 泛型对应的表
    // mabatis-plus ; 类似于mybatis ---- mybatis 对 jdbc 封装！
    @Autowired
    private BaseCategory1Mapper baseCategory1Mapper;

    @Autowired
    private BaseCategory2Mapper baseCategory2Mapper;

    @Autowired
    private BaseCategory3Mapper baseCategory3Mapper;

    @Autowired
    private BaseAttrInfoMapper baseAttrInfoMapper;

    @Autowired
    private BaseAttrValueMapper baseAttrValueMapper;

    @Autowired
    private SpuInfoMapper spuInfoMapper;

    @Autowired
    private BaseSaleAttrMapper baseSaleAttrMapper;

    @Autowired
    private SpuImageMapper spuImageMapper;

    @Autowired
    private SpuPosterMapper spuPosterMapper;

    @Autowired
    private SpuSaleAttrMapper spuSaleAttrMapper;

    @Autowired
    private SpuSaleAttrValueMapper spuSaleAttrValueMapper;

    @Autowired
    private SkuInfoMapper skuInfoMapper;

    @Autowired
    private SkuImageMapper skuImageMapper;

    @Autowired
    private SkuSaleAttrValueMapper skuSaleAttrValueMapper;

    @Autowired
    private SkuAttrValueMapper skuAttrValueMapper;

    @Autowired
    private BaseCategoryViewMapper baseCategoryViewMapper;

    @Override
    public List<BaseCategory1> getCategory1() {
        //select * from base_category1 where is_deleted = 0;
        return baseCategory1Mapper.selectList(null);
    }

    @Override
    public List<BaseCategory2> getCategory2(Long category1Id) {
        //调用mapper 查询数据
        //select * from base_category2 where category1_id = ? and is_deleted = 0;
        return baseCategory2Mapper.selectList(new QueryWrapper<BaseCategory2>().eq("category1_id", category1Id));
    }

    @Override
    public List<BaseCategory3> getCategory3(Long category2Id) {
        //select * from base_category3 where category2_id = ? and is_deleted = 0;
        return baseCategory3Mapper.selectList(new QueryWrapper<BaseCategory3>().eq("category2_id", category2Id));
    }

    @Override
    public List<BaseAttrInfo> getAttrInfoList(Long category1Id, Long category2Id, Long category3Id) {
        // 调用mapper
        return baseAttrInfoMapper.selectAttrInfoList(category1Id, category2Id, category3Id);
    }
    @Override
    @Transactional(rollbackFor = Exception.class)//当发生异常的时候直接回滚数据
    public void saveAttrInfo(BaseAttrInfo baseAttrInfo) {
        //判断什么时候修改，什么时候新增
        if (baseAttrInfo.getId()!=null){
            //修改 base_attr_info
            baseAttrInfoMapper.updateById(baseAttrInfo);
            //修改的时候删除数据；逻辑删除 本质是 update base_attr_value set is_deleted = 1 where attr_id = ?
            QueryWrapper<BaseAttrValue> baseAttrValueQueryWrapper = new QueryWrapper<>();
            baseAttrValueQueryWrapper.eq("attr_id", baseAttrInfo.getId());
            baseAttrValueMapper.delete(baseAttrValueQueryWrapper);
        }else {
            //新增
            //保存数据：base_attr_info
            baseAttrInfoMapper.insert(baseAttrInfo);
        }
        //保存数据：base_attr_info base_attr_value
        //baseAttrInfoMapper.insert(baseAttrInfo);
        //从baseAttrInfo.getAttrValueList();
        //获取到平台属性值集合
        List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
        if (!CollectionUtils.isEmpty(attrValueList)){
            //Consumer 函数式接口：有参无返回，那么参数是谁？由什么来决定？由当前这个集合对应的泛型决定
            attrValueList.forEach(baseAttrValue -> {
                //attr_id = base_attr_info.id;页面传递的时候没有传递attr_id,所以要在此主动赋值
                //baseAttrInfo.getId() 能够获取到主键自增的Id? IdType.AUTO 自动获取到主键自增
                //前提：这个表必须先执行insert
                baseAttrValue.setAttrId(baseAttrInfo.getId());
                baseAttrValueMapper.insert(baseAttrValue);
            });
        }
    }

    @Override
    public List<BaseAttrValue> getAttrValueList(Long attrId) {
        //根据平台属性Id 回显平台属性集合 selest * from base_attr_value where attr_id = ?
        //Wrapper -- 封装[查询，修改，删除]条件
        //操作的哪张表，就对应写那个表的实体类
        QueryWrapper<BaseAttrValue> baseAttrValueQueryWrapper = new QueryWrapper<>();
        baseAttrValueQueryWrapper.eq("attr_id", attrId);
        return baseAttrValueMapper.selectList(baseAttrValueQueryWrapper);
    }

    @Override
    public BaseAttrInfo getAttrInfo(Long attrId) {
        //根据主键查询数据
        BaseAttrInfo baseAttrInfo = baseAttrInfoMapper.selectById(attrId);
        if (baseAttrInfo!=null){
            //查询平台属性值集合数据并赋值
            baseAttrInfo.setAttrValueList(this.getAttrValueList(attrId));
        }
        //返回平台属性
        return baseAttrInfo;
    }

    @Override
    public IPage<SpuInfo> getSpuList(Page<SpuInfo> spuInfoPage, SpuInfo spuInfo) {
        //select * from spu_info where category3_id = 61 order by id desc limit 0,10  #第二页
        QueryWrapper<SpuInfo> spuInfoQueryWrapper = new QueryWrapper<>();
        spuInfoQueryWrapper.eq("category3_id", spuInfo.getCategory3Id());
        spuInfoQueryWrapper.orderByDesc("id");
        return spuInfoMapper.selectPage(spuInfoPage, spuInfoQueryWrapper);
    }

    @Override
    public List<BaseSaleAttr> getBaseSaleAttrList() {
        // select * from base_sale_attr where is_delete = 0;
        return baseSaleAttrMapper.selectList(null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveSpuInfo(SpuInfo spuInfo) {
        //保存本质 ：insert
        //        spu_info
        spuInfoMapper.insert(spuInfo);

        //        spu_image
        List<SpuImage> spuImageList = spuInfo.getSpuImageList();
        if (!CollectionUtils.isEmpty(spuImageList)){
            spuImageList.forEach(spuImage -> {
                // 细节：spuId
                spuImage.setSpuId(spuInfo.getId());
                spuImageMapper.insert(spuImage);
            });
        }
        //        spu_poster
        List<SpuPoster> spuPosterList = spuInfo.getSpuPosterList();
        if (!CollectionUtils.isEmpty(spuPosterList)){
            spuPosterList.forEach(spuPoster -> {
                spuPoster.setSpuId(spuInfo.getId());
                spuPosterMapper.insert(spuPoster);
            });
        }
        //        spu_sale_attr
        List<SpuSaleAttr> spuSaleAttrList = spuInfo.getSpuSaleAttrList();
        if (!CollectionUtils.isEmpty(spuImageList)){
            spuSaleAttrList.forEach(spuSaleAttr -> {
                spuSaleAttr.setSpuId(spuInfo.getId());
                spuSaleAttrMapper.insert(spuSaleAttr);
                //        spu_sale_attr_value
                List<SpuSaleAttrValue> spuSaleAttrValueList = spuSaleAttr.getSpuSaleAttrValueList();
                if (!CollectionUtils.isEmpty(spuSaleAttrValueList)){
                    spuSaleAttrValueList.forEach(spuSaleAttrValue -> {

                        spuSaleAttrValue.setSpuId(spuInfo.getId());
                        spuSaleAttrValue.setSaleAttrName(spuSaleAttr.getSaleAttrName());
                        spuSaleAttrValueMapper.insert(spuSaleAttrValue);
                    });
                }
            });
        }
    }
    @Override
    public List<SpuImage> getSpuImageList(Long spuId) {
        // select * from spu_image where spu_id = ? and is_delete = 0;
        return spuImageMapper.selectList(new QueryWrapper<SpuImage>().eq("spu_id", spuId));
    }

    @Override
    public List<SpuSaleAttr> getSpuSaleAttrList(Long spuId) {
        // 调用mapper层@
        return spuSaleAttrMapper.selectSpuSaleAttrList(spuId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class) //默认是运行时异常，只要有异常就回滚！
    public void saveSkuInfo(SkuInfo skuInfo) {
        // 判断是否为空，你要做修改吗？有修改才判断，如果没有修改，则直接insert!
        try {
            //        sku_info
            this.skuInfoMapper.insert(skuInfo);
            //        sku_image
            List<SkuImage> skuImageList = skuInfo.getSkuImageList();
            if (!CollectionUtils.isEmpty(skuImageList)){
                skuImageList.forEach(skuImage -> {
                    skuImage.setSkuId(skuInfo.getId());
                    skuImageMapper.insert(skuImage);
                });
            }
            //      sku_attr_value  平台属性数据
            List<SkuAttrValue> skuAttrValueList = skuInfo.getSkuAttrValueList();
            if (!CollectionUtils.isEmpty(skuAttrValueList)){
                skuAttrValueList.forEach(skuAttrValue -> {
                    // 细节处理skuId
                    skuAttrValue.setSkuId(skuInfo.getId());
                    skuAttrValueMapper.insert(skuAttrValue);
                });
            }
            //        sku_sale_attr_value
            List<SkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();
            if (!CollectionUtils.isEmpty(skuSaleAttrValueList)){
                skuSaleAttrValueList.forEach(skuSaleAttrValue -> {
                    skuSaleAttrValue.setSkuId(skuInfo.getId());
                    skuSaleAttrValue.setSpuId(skuInfo.getSpuId());
                    skuSaleAttrValueMapper.insert(skuSaleAttrValue);
                });
            }
        } catch (Exception e) {
            // 最后记录日志信息
            e.printStackTrace();
        }
    }

    @Override
    public IPage getskuInfoList(Page<SkuInfo> skuInfoPage, SkuInfo skuInfo) {
        // select * from skuInfo where category3_id = ? and is_delete = 0 order by id desc limit 0, 10;
        //构建条件
        QueryWrapper<SkuInfo> skuInfoQueryWrapper = new QueryWrapper<>();
        skuInfoQueryWrapper.eq("category3_id", skuInfo.getCategory3Id());
        skuInfoQueryWrapper.orderByDesc("id");
        return skuInfoMapper.selectPage(skuInfoPage, skuInfoQueryWrapper);
    }

    @Override
    public void onSale(Long skuId) {
        // is_sale = 1
        //  update sku_info set is_sale = 1 where id = skuId and is_delete = 0;
        SkuInfo skuInfo = new SkuInfo();
        skuInfo.setId(skuId);
        skuInfo.setIsSale(1);
        this.skuInfoMapper.updateById(skuInfo);
    }

    @Override
    public void cancelSale(Long skuId) {
        // is_sale = 0
        SkuInfo skuInfo = new SkuInfo();
        skuInfo.setId(skuId);
        skuInfo.setIsSale(0);
        this.skuInfoMapper.updateById(skuInfo);
    }

    @Override
    public SkuInfo getSkuInfo(Long skuId) {
        //  select * from sku_info where id = ?;
        //        QueryWrapper<SkuInfo> skuInfoQueryWrapper = new QueryWrapper<>();
        //        skuInfoQueryWrapper.eq("id", skuId);
        SkuInfo skuInfo = skuInfoMapper.selectById(skuId);
        //  select * from sku_image where sku_id = skuId;
        //  QueryWrapper 构建非主键查询条件
        QueryWrapper<SkuImage> skuImageQueryWrapper = new QueryWrapper<>();
        skuImageQueryWrapper.eq("sku_id", skuId);
        List<SkuImage> skuImageList = skuImageMapper.selectList(skuImageQueryWrapper);
        skuInfo.setSkuImageList(skuImageList);
        //  返回数据
        return skuInfo;
    }

    @Override
    public BaseCategoryView getBaseCategoryView(Long category3Id) {
        //select * from base_category_view where id = 61;
        return baseCategoryViewMapper.selectById(category3Id);
    }

    @Override
    public BigDecimal getSkuPrice(Long skuId) {
        //  select price from sku_info where id = 24;
        QueryWrapper<SkuInfo> skuInfoQueryWrapper = new QueryWrapper<>();
        skuInfoQueryWrapper.eq("id", skuId);
        //  设置查询字段 price
        skuInfoQueryWrapper.select("price");
        SkuInfo skuInfo = skuInfoMapper.selectOne(skuInfoQueryWrapper);
        if (skuInfo!=null){
            return skuInfo.getPrice();
        }
        //  select * from sku_info where id = 24;
        //  SkuInfo skuInfo = skuInfoMapper.selectById(skuId);
        //  返回最新价格
        return new BigDecimal("0");
    }

    @Override
    public List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(Long skuId, Long spuId) {
        //  调用mapper
        return spuSaleAttrMapper.selectSpuSaleAttrListCheckBySku(skuId,spuId);
    }

    @Override
    public Map getSkuValueIdsMap(Long spuId) {
        //  声明一个map集合
        HashMap<Object, Object> hashMap = new HashMap<>();
        //  调用mappper 获取数据  难点一：返回结果类型 map -- 可以代替实体类！ key = property
        List<Map> mapList = skuSaleAttrValueMapper.selectSkuValueIdsMap(spuId);
        if (!CollectionUtils.isEmpty(mapList)){
            mapList.forEach(map -> {
                //  难点二：通过列名获取数据
                hashMap.put(map.get("value_ids"), map.get("sku_id"));
            });
        }
        //  返回数据
        return hashMap;
    }

    @Override
    public List<SpuPoster> findSpuPosterBySpuId(Long spuId) {
        return spuPosterMapper.selectList(new QueryWrapper<SpuPoster>().eq("spu_id", spuId));
    }

    @Override
    public List<BaseAttrInfo> getAttrList(Long skuId) {
        //调用mapper
        return baseAttrInfoMapper.selectAttrList(skuId);
    }
}
