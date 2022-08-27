package com.atguigu.gmall.product.service.impl;

import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.mapper.*;
import com.atguigu.gmall.product.service.ManagerService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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
        //保存数据：base_attr_info base_attr_value
        baseAttrInfoMapper.insert(baseAttrInfo);
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
}
