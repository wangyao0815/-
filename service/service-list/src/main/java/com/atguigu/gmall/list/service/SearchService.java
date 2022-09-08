package com.atguigu.gmall.list.service;

import com.atguigu.gmall.list.SearchParam;
import com.atguigu.gmall.list.SearchResponseVo;

import java.io.IOException;

public interface SearchService {

    //上架：参数 返回值
    void upperGoods(Long skuId);
    // 下架
    void lowerGoods(Long skuId);
    // 更新es 热度
    void incrHotScore(Long skuId);

    //查询
    SearchResponseVo search(SearchParam searchParam) throws IOException;
}
