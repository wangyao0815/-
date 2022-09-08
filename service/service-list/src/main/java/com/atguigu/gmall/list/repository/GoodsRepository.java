package com.atguigu.gmall.list.repository;

import com.atguigu.gmall.list.Goods;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface GoodsRepository extends ElasticsearchRepository<Goods,Long> {
}
