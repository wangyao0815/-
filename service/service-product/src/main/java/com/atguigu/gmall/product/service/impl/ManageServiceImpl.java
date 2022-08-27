package com.atguigu.gmall.product.service.impl;

import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.mapper.*;
import com.atguigu.gmall.product.service.ManagerService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jodd.util.Consumers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Service
public class ManageServiceImpl implements ManagerService {

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
}
