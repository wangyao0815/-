package com.atguigu.gmall.product.service;

import com.atguigu.gmall.model.product.BaseTrademark;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;

public interface BaseTrademarkService {
    /**
     * 品牌分页列表
     * @param baseTrademarkPage
     * @return
     */
    IPage getTradeMarkList(Page<BaseTrademark> baseTrademarkPage);
}
