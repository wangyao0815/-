package com.atguigu.gmall.product.service.impl;

import com.atguigu.gmall.model.product.BaseCategoryTrademark;
import com.atguigu.gmall.model.product.BaseTrademark;
import com.atguigu.gmall.model.product.CategoryTrademarkVo;
import com.atguigu.gmall.product.mapper.BaseCategoryTrademarkMapper;
import com.atguigu.gmall.product.mapper.BaseTrademarkMapper;
import com.atguigu.gmall.product.service.BaseCategoryTrademarkService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BaseCategoryTrademarkServiceImpl extends ServiceImpl<BaseCategoryTrademarkMapper,BaseCategoryTrademark> implements BaseCategoryTrademarkService {
    // 调用mapper 层
    @Autowired
    private BaseTrademarkMapper baseTrademarkMapper;

    @Autowired
    private BaseCategoryTrademarkMapper baseCategoryTrademarkMapper;

    @Override
    public List<BaseTrademark> findTrademarkList(Long category3Id) {
        //base_trademark 这个表中没有三级分类Id
        //已知三级分类Id 求 品牌列表
        //base_category_trademark 记录哪个分类下有哪些品牌  base_category_trademark 获取到品牌Id
        //select *  from base_category_trademark where category3_id = 61 and is_deleted = 0;
        QueryWrapper<BaseCategoryTrademark> baseCategoryTrademarkQueryWrapper = new QueryWrapper<>();
        baseCategoryTrademarkQueryWrapper.eq("category3_id",category3Id);
        List<BaseCategoryTrademark> baseCategoryTrademarkList = baseCategoryTrademarkMapper.selectList(baseCategoryTrademarkQueryWrapper);

        // 普通for循环方式
        //        ArrayList<Long> tmIdList = new ArrayList<>();
        //        //要使用这个结果集中的品牌Id
        //        if (!CollectionUtils.isEmpty(baseCategoryTrademarkList)){
        //            baseCategoryTrademarkList.forEach(baseCategoryTrademark -> {
        //                Long trademarkId = baseCategoryTrademark.getTrademarkId();
        //                tmIdList.add(trademarkId);
        //            });
        //            //获取到的数据
        //            List<BaseTrademark> baseTrademarkList = this.baseTrademarkMapper.selectBatchIds(tmIdList);
        //            // 返回数据
        //            return baseTrademarkList;
        //        }

        if (!CollectionUtils.isEmpty(baseCategoryTrademarkList)){
            // java 基础中讲的流式编程
            //          List<Long> tmIdList = baseCategoryTrademarkList.stream().map(baseCategoryTrademark -> {
            //              return baseCategoryTrademark.getTrademarkId();
            //          }).collect(Collectors.toList());

            // BaseCategoryTrademark::getTrademarkId  方法引用
            List<Long> tmIdsList = baseCategoryTrademarkList.stream().map(BaseCategoryTrademark::getTrademarkId).collect(Collectors.toList());
            //获取到的数据
            List<BaseTrademark> baseTrademarkList = this.baseTrademarkMapper.selectBatchIds(tmIdsList);
            return baseTrademarkList;
        }
        return null;
    }

    @Override
    public List<BaseTrademark> findCurrentTrademarkList(Long category3Id) {
        /*
            思路：
                1.  先查所有tmId          baseTrademarkMapper.selectList(null);
                2.  再查已绑定tmId
                3.  去重
         */
        //已知三级分类Id 求 品牌列表
        //base_category_trademark 记录哪个分类下有哪些品牌  base_category_trademark 获取到品牌Id
        //select *  from base_category_trademark where category3_id = 61 and is_deleted = 0;
        QueryWrapper<BaseCategoryTrademark> baseCategoryTrademarkQueryWrapper = new QueryWrapper<>();
        baseCategoryTrademarkQueryWrapper.eq("category3_id",category3Id);
        List<BaseCategoryTrademark> baseCategoryTrademarkList = baseCategoryTrademarkMapper.selectList(baseCategoryTrademarkQueryWrapper);

        if (!CollectionUtils.isEmpty(baseCategoryTrademarkList)){
            // 获取到已经绑定的品牌Id  1,3
            List<Long> tmIdsList = baseCategoryTrademarkList.stream().map(BaseCategoryTrademark::getTrademarkId).collect(Collectors.toList());

            // 查询所有品牌数据，然后再去重  map:映射关系
            // 参数类型是谁： 集合的泛型
            //baseTrademarkMapper.selectList(null) 1，2，3，5，6 tmIdsList：1，3，2
            List<BaseTrademark> baseTrademarkList = baseTrademarkMapper.selectList(null).stream().filter(baseTrademark -> {
                return !tmIdsList.contains(baseTrademark.getId());
            }).collect(Collectors.toList());
            //返回数据
            return baseTrademarkList;
        }
        // 如果没有已绑定的品牌Id，则查询所有数据
        return baseTrademarkMapper.selectList(null);
    }

    @Override
    public void save(CategoryTrademarkVo categoryTrademarkVo) {
        //base_category_trademark
        //获取要绑定的 tmId
        List<Long> trademarkIdList = categoryTrademarkVo.getTrademarkIdList();
        //第一种方式：循环遍历执行insert into
        //        if (!CollectionUtils.isEmpty(trademarkIdList)){
        //            // 循环遍历
        //            trademarkIdList.forEach(tmId ->{
        //                BaseCategoryTrademark baseCategoryTrademark = new BaseCategoryTrademark();
        //                baseCategoryTrademark.setCategory3Id(categoryTrademarkVo.getCategory3Id());
        //                baseCategoryTrademark.setTrademarkId(tmId);
        //                //循环遍历执行insert into 语句；
        //                baseCategoryTrademarkMapper.insert(baseCategoryTrademark);
        //            });
        //        }


        //ArrayList<BaseCategoryTrademark> baseCategoryTrademarkArrayList = new ArrayList<>();
        // 第二种方式：批量操作
        if (!CollectionUtils.isEmpty(trademarkIdList)){
            // 循环遍历获取元素，然后添加到集合中
            //            trademarkIdList.forEach(tmId ->{
            //                BaseCategoryTrademark baseCategoryTrademark = new BaseCategoryTrademark();
            //                baseCategoryTrademark.setCategory3Id(categoryTrademarkVo.getCategory3Id());
            //                baseCategoryTrademark.setTrademarkId(tmId);
            //                baseCategoryTrademarkArrayList.add(baseCategoryTrademark);
            //            });

            List<BaseCategoryTrademark> baseCategoryTrademarkArrayList = trademarkIdList.stream().map(tmId -> {
                BaseCategoryTrademark baseCategoryTrademark = new BaseCategoryTrademark();
                baseCategoryTrademark.setCategory3Id(categoryTrademarkVo.getCategory3Id());
                baseCategoryTrademark.setTrademarkId(tmId);
                return baseCategoryTrademark;
            }).collect(Collectors.toList());
            //放入集合数据
            this.saveBatch(baseCategoryTrademarkArrayList);
        }
    }

    @Override
    public void removeByCategory3IdAndTmId(Long category3Id, Long tmId) {
        //update base_category_trademark set is-deleted = 1 where category3_id = ? and trademark_id = ?
        QueryWrapper<BaseCategoryTrademark> baseCategoryTrademarkQueryWrapper = new QueryWrapper<>();
        baseCategoryTrademarkQueryWrapper.eq("category3_id", category3Id);
        baseCategoryTrademarkQueryWrapper.eq("trademark_id", tmId);
        baseCategoryTrademarkMapper.delete(baseCategoryTrademarkQueryWrapper);
    }
}
