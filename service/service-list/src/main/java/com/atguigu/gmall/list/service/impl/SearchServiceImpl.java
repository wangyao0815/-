package com.atguigu.gmall.list.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.list.*;
import com.atguigu.gmall.list.repository.GoodsRepository;
import com.atguigu.gmall.list.service.SearchService;
import com.atguigu.gmall.model.product.BaseAttrInfo;
import com.atguigu.gmall.model.product.BaseCategoryView;
import com.atguigu.gmall.model.product.BaseTrademark;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.servlet.http.Cookie;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SearchServiceImpl implements SearchService {

    @Autowired
    private ProductFeignClient productFeignClient;

    // 商品上架 --- 将数据封装到Goods,并且将Goods 保存到es中
    @Autowired
    private GoodsRepository goodsRepository;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    @Override
    public void upperGoods(Long skuId) {

        Cookie cookie = new Cookie("name", "刘德华");
        cookie.setDomain("host");
        cookie.setPath("/");

        Goods goods = new Goods();
        //  给goods 赋值
        //  远程调用 获取对应的数据
        SkuInfo skuInfo = this.productFeignClient.getSkuInfo(skuId);
        goods.setId(skuId);
        goods.setTitle(skuInfo.getSkuName());
        goods.setDefaultImg(skuInfo.getSkuDefaultImg());
        //  skuInfo 可能是从缓存中获取的数据；延迟双删 --->避免出现脏读
        //  goods.setPrice(skuInfo.getPrice().doubleValue());
        goods.setPrice(this.productFeignClient.getSkuPrice(skuId).doubleValue());
        goods.setCreateTime(new Date());

        //  赋值品牌数据
        BaseTrademark trademark = this.productFeignClient.getTrademark(skuInfo.getTmId());
        goods.setTmId(trademark.getId());
        goods.setTmName(trademark.getTmName());
        goods.setTmLogoUrl(trademark.getLogoUrl());

        //  赋值分类数据
        BaseCategoryView categoryView = this.productFeignClient.getCategoryView(skuInfo.getCategory3Id());

        goods.setCategory1Id(categoryView.getCategory1Id());
        goods.setCategory2Id(categoryView.getCategory2Id());
        goods.setCategory3Id(categoryView.getCategory3Id());

        goods.setCategory1Name(categoryView.getCategory1Name());
        goods.setCategory2Name(categoryView.getCategory2Name());
        goods.setCategory3Name(categoryView.getCategory3Name());

        //  热度排名

        //  赋值平台属性  List<SearchAttr> attrs
        List<BaseAttrInfo> attrList = this.productFeignClient.getAttrList(skuId);

        List<SearchAttr> searchAttrList = attrList.stream().map(baseAttrInfo -> {
            SearchAttr searchAttr = new SearchAttr();
            searchAttr.setAttrId(baseAttrInfo.getId());
            searchAttr.setAttrName(baseAttrInfo.getAttrName());
            //  因为 skuId 对应的平台属性 --- 属性值只有一个
            searchAttr.setAttrValue(baseAttrInfo.getAttrValueList().get(0).getValueName());
            return searchAttr;
        }).collect(Collectors.toList());

        //  赋值成功
        goods.setAttrs(searchAttrList);

        //  实现了商品上架
        this.goodsRepository.save(goods);
    }

    /**
     * 下架
     * @param skuId
     */
    @Override
    public void lowerGoods(Long skuId) {
        this.goodsRepository.deleteById(skuId);
    }

    @Override
    public void incrHotScore(Long skuId) {
        /*
        redis:三个问题 + 数据类型
        count:被访问的次数 -------  redis
        if(count % 100 ==0){
           updayeEs();
         }

         String incr decr
         ZSet  zincrby  hotScore  increment  member --- 首选

         String , ZSet , 【Hash , List】
        */

        String hotKey = "hotScore";
        //调用方法
        Double count = this.redisTemplate.opsForZSet().incrementScore(hotKey, "skuId:" + skuId, 1);
        if (count%10==0){
            //更新es
            Optional<Goods> optional = this.goodsRepository.findById(skuId);
            Goods goods = optional.get();
            goods.setHotScore(count.longValue());
            this.goodsRepository.save(goods);
        }

    }

    @Override
    public SearchResponseVo search(SearchParam searchParam) throws IOException {
        /*
            检索 动态生成dsl 语句
            1.先生成dsl 语句     SearchRequest
            2.执行dsl 语句       SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
            3.将执行之后的结果进行封装  SearchResponseVo
         */
        // 声明一个查询请求对象
        SearchRequest searchRequest = this.bulidDsl(searchParam);
        // 执行dsl 语句
        SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        // 将执行之后的结果进行封装  SearchResponseVo
        SearchResponseVo searchResponseVo = this.parseResult(searchResponse);
        //  本质就是给属性赋值
        //        private List<SearchResponseTmVo> trademarkList;
        //        private List<SearchResponseAttrVo> attrsList = new ArrayList<>();
        //        private List<Goods> goodsList = new ArrayList<>();
        //        private Long total;//总记录数
        // --------------------以上四个参数在 parseResult() 方法中赋值------


        //        private Integer pageSize;//每页显示的内容
        //        private Integer pageNo;//当前页面
        //        private Long totalPages;
        //  默认设置每页显示条数：3
        searchResponseVo.setPageSize(searchParam.getPageSize());
        searchResponseVo.setPageNo(searchParam.getPageNo());
        //  显示3，4条数据
        //  总共10条数据 每页3条 4 9 3 3
        //  Long totalPages = searchResponseVo.getTotal()%searchParam.getPageSize()==0?searchResponseVo.getTotal()/searchParam.getPageSize():searchResponseVo.getTotal()/searchParam.getPageSize()+1;
        Long totalPages =(searchResponseVo.getTotal()+searchParam.getPageSize()-1)/searchParam.getPageSize();
        //  from = (pageNo-1)*PageSize
        searchResponseVo.setTotalPages(totalPages);
        //返回数据
        return searchResponseVo;
    }

    /**
     * 设置返回数据
     * @param searchResponse
     * @return
     */
    private SearchResponseVo parseResult(SearchResponse searchResponse) {
        //  声明对象
        SearchResponseVo searchResponseVo = new SearchResponseVo();
        //        private List<SearchResponseTmVo> trademarkList;
        //        private List<SearchResponseAttrVo> attrsList = new ArrayList<>();
        //        private List<Goods> goodsList = new ArrayList<>();
        //        private Long total;//总记录数
        SearchHits hits = searchResponse.getHits();
        //  设置总记录数
        searchResponseVo.setTotal(hits.getTotalHits().value);
        //  设置商品的集合
        //  声明一个集合来存储 商品对象
        List<Goods> goodsList = new ArrayList<>();
        SearchHit[] subHits = hits.getHits();
        if (subHits!=null && subHits.length>0){
            for (SearchHit subHit : subHits) {
                //  获取到source 字符串
                String sourceAsString = subHit.getSourceAsString();
                //  将其转换为Goods
                Goods goods = JSON.parseObject(sourceAsString, Goods.class);
                //  判断用户是否根据关键词进行了检索，如果是则获取高亮字段
                if (subHit.getHighlightFields().get("title")!=null){
                    //  说明有高亮
                    Text title = subHit.getHighlightFields().get("title").getFragments()[0];
                    goods.setTitle(title.toString());
                }
                //  添加商品到集合
                goodsList.add(goods);
            }
        }
        searchResponseVo.setGoodsList(goodsList);

        //  获取品牌数据
        Map<String, Aggregation> aggregationMap = searchResponse.getAggregations().asMap();
        //  应该获取桶中的数据
        ParsedLongTerms tmIdAgg = (ParsedLongTerms) aggregationMap.get("tmIdAgg");
        //  循环遍历时，获取品牌Id给   searchResponseTmVo 对象，然后再将这个对象添加到集合中！
        List<SearchResponseTmVo> trademarkList = tmIdAgg.getBuckets().stream().map(bucket -> {
            SearchResponseTmVo searchResponseTmVo = new SearchResponseTmVo();
            //  获取品牌Id
            String tmId = ((Terms.Bucket) bucket).getKeyAsString();
            searchResponseTmVo.setTmId(Long.parseLong(tmId));

            //  获取品牌Name
            ParsedStringTerms tmNameAgg = ((Terms.Bucket) bucket).getAggregations().get("tmNameAgg");
            String tmName = tmNameAgg.getBuckets().get(0).getKeyAsString();
            searchResponseTmVo.setTmName(tmName);

            //  获取品牌LogoUrl
            ParsedStringTerms tmLogoUrlAgg = ((Terms.Bucket) bucket).getAggregations().get("tmLogoUrlAgg");
            String tmLogoUrl = tmLogoUrlAgg.getBuckets().get(0).getKeyAsString();
            searchResponseTmVo.setTmLogoUrl(tmLogoUrl);

            return searchResponseTmVo;
        }).collect(Collectors.toList());
        //  设置品牌集合数据
        searchResponseVo.setTrademarkList(trademarkList);

        //  平台属性值：数据类型nested
        ParsedNested attrAgg = (ParsedNested) aggregationMap.get("attrAgg");
        ParsedLongTerms attrIdAgg = attrAgg.getAggregations().get("attrIdAgg");
    //  循环遍历获取平台属性Id，平台属性名，平台属性集合！并返回集合
        List<SearchResponseAttrVo> attrsList = attrIdAgg.getBuckets().stream().map(bucket -> {
            //  声明一个平台属性对象
            SearchResponseAttrVo searchResponseAttrVo = new SearchResponseAttrVo();
            //  获取平台属性Id
            String attrId = ((Terms.Bucket) bucket).getKeyAsString();
            searchResponseAttrVo.setAttrId(Long.parseLong(attrId));
            //  获取平台属性名
            ParsedStringTerms attrNameAgg = ((Terms.Bucket) bucket).getAggregations().get("attrNameAgg");
            String attrName = attrNameAgg.getBuckets().get(0).getKeyAsString();
            searchResponseAttrVo.setAttrName(attrName);
            //  获取平台属性集合
            ParsedStringTerms attrValueAgg = ((Terms.Bucket) bucket).getAggregations().get("attrValueAgg");
            //            List<? extends Terms.Bucket> buckets = attrValueAgg.getBuckets();
            //            ArrayList<String> list = new ArrayList<>();
            //            buckets.forEach(value->{
            //                String valueName = value.getKeyAsString();
            //                list.add(valueName);
            //            });
            //            searchResponseAttrVo.setAttrValueList(list);
            //  方法引用
            List<String> valueList = attrValueAgg.getBuckets().stream().map(Terms.Bucket::getKeyAsString).collect(Collectors.toList());
            //  赋值平台属性值集合
            searchResponseAttrVo.setAttrValueList(valueList);
            //  返回平台属性对象
            return searchResponseAttrVo;
        }).collect(Collectors.toList());


        searchResponseVo.setAttrsList(attrsList);
        //  返回对象
        return searchResponseVo;
    }

    /**
     * 动态生成dsl 语句返回请求对象
     * @param searchParam
     * @return
     */
    private SearchRequest bulidDsl(SearchParam searchParam) {
        //  构建查询器{}
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        // {bool }
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        //  判断用户是否是根据三级分类Id进行检索
        if (!StringUtils.isEmpty(searchParam.getCategory3Id())){
            //{filter term}
            boolQueryBuilder.filter(QueryBuilders.termQuery("category3Id", searchParam.getCategory3Id()));
        }
        if (!StringUtils.isEmpty(searchParam.getCategory2Id())){
            //{filter term}
            boolQueryBuilder.filter(QueryBuilders.termQuery("category2Id", searchParam.getCategory2Id()));
        }
        if (!StringUtils.isEmpty(searchParam.getCategory1Id())){
            //{filter term}
            boolQueryBuilder.filter(QueryBuilders.termQuery("category1Id", searchParam.getCategory1Id()));
        }

        //根据关键词来检索
        if (!StringUtils.isEmpty(searchParam.getKeyword())){
            //{filter must match}
            boolQueryBuilder.must(QueryBuilders.matchQuery("title",searchParam.getKeyword()).operator(Operator.AND));

            //  设置高亮
            HighlightBuilder highlightBuilder = new HighlightBuilder();
            highlightBuilder.field("title").preTags("<span style=color:red>").postTags("</span>");

            //HighlightBuilder highlightBuilder = searchSourceBuilder.highlighter().field("title").preTags("<span style=color:red>").postTags("</span>");
            searchSourceBuilder.highlighter(highlightBuilder);
        }

        //  根据平台属性值Id进行过滤
        //  先获取到用户点击的数据
        String[] props = searchParam.getProps();
        if (props!=null && props.length>0){
            for (String prop : props) {
                //  第一次遍历 prop = 24：256：机身内存
                String[] split = prop.split(":");
                //  声明中间层的bool
                BoolQueryBuilder boolBuilder = QueryBuilders.boolQuery();
                //  声明内层的bool
                BoolQueryBuilder innerBoolBuilder = QueryBuilders.boolQuery();
                //设置内层
                innerBoolBuilder.must(QueryBuilders.matchQuery("attrs.attrId",split[0]));
                innerBoolBuilder.must(QueryBuilders.matchQuery("attrs.attrValue",split[1]));
                //设置中间层
                boolBuilder.must(QueryBuilders.nestedQuery("attrs", innerBoolBuilder, ScoreMode.None));
                boolQueryBuilder.filter(boolBuilder);

                //boolQueryBuilder.filter(QueryBuilders.nestedQuery("attrs", innerBoolBuilder, ScoreMode.None));
            }
        }

        // 根据品牌Id 进行检索
        String trademark = searchParam.getTrademark();
        if (!StringUtils.isEmpty(trademark)){
            //  分割
            String[] split = trademark.split(":");
            //判断
            if (split!=null&&split.length==2){
                //  {filter term}
                boolQueryBuilder.filter(QueryBuilders.termQuery("tmId", split[0]));
            }
        }

        //{query  --  bool}
        searchSourceBuilder.query(boolQueryBuilder);
        //  分页，高亮，排序，聚合
        //  from 表示当前的起始条数
        int from = (searchParam.getPageNo()-1)*searchParam.getPageSize();
        searchSourceBuilder.from(from);
        //默认显示3条
        searchSourceBuilder.size(searchParam.getPageSize());

        //排序 规则不唯一，根据页面传递的数据决定的
        // order=1:asc  order=1:desc  order=2:asc  order=2:desc
        String order = searchParam.getOrder();
        if (!StringUtils.isEmpty(order)){
            // 分割order
            String[] split = order.split(":");
            if (split!=null && split.length==2){
                //  需要知道排序的字段与规则，排序的字段是由第一个参数决定，规则是由第二个参数决定
                String field = "";
                // 1=hotScore   2 = price
                switch (split[0]){
                    case "1":
                        field = "hotScore";
                        //searchSourceBuilder.sort("hotScore","asc".equals(split[1])? SortOrder.ASC:SortOrder.DESC);
                        break;
                    case "2":
                        field = "price";
                        //searchSourceBuilder.sort("hotScore","asc".equals(split[1])? SortOrder.ASC:SortOrder.DESC);
                        break;
                }
                searchSourceBuilder.sort(field,"asc".equals(split[1])? SortOrder.ASC:SortOrder.DESC);
            }
        }else {
            searchSourceBuilder.sort("hotScore",SortOrder.DESC);
        }

        //  聚合：去重，显示！ 提供检索条件
        //  第一部分：品牌
        searchSourceBuilder.aggregation(AggregationBuilders.terms("tmIdAgg").field("tmId")
            .subAggregation(AggregationBuilders.terms("tmNameAgg").field("tmName"))
            .subAggregation(AggregationBuilders.terms("tmLogoUrlAgg").field("tmLogoUrl")));
        //  第二部分：平台属性 ---- 数据类型 nested
        searchSourceBuilder.aggregation(AggregationBuilders.nested("attrAgg", "attrs")
            .subAggregation(AggregationBuilders.terms("attrIdAgg").field("attrs.attrId")
                .subAggregation(AggregationBuilders.terms("attrNameAgg").field("attrs.attrName"))
                .subAggregation(AggregationBuilders.terms("attrValueAgg").field("attrs.attrValue"))));
        //  声明一个请求对象  GET /goods/_search 在哪个索引库查询
        SearchRequest searchRequest = new SearchRequest("goods");
        //  将dsl 语句赋值给查询对象
        searchRequest.source(searchSourceBuilder);
        //  看到dsl 语句
        System.out.println("dsl:\t" + searchSourceBuilder.toString());
        //  设置哪些field 需要展示哪些数据，哪些field 不需要展示数据
        searchSourceBuilder.fetchSource(new String[]{"id","defaultImg","title","price","createTime"},null);
        //  返回数据
        return searchRequest;
    }
}
