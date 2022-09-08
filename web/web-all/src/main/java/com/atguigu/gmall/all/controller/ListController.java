package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.client.ListFeignClient;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.list.SearchAttr;
import com.atguigu.gmall.list.SearchParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ListController {

    @Autowired
    private ListFeignClient listFeignClient;

    //  通过浏览器会将用户输入的检索条件，直接封装到这个实体类  --- 为什么能封装进去？
    //  控制器后面传入的参数 与 实体类的属性名一样
    @GetMapping("list.html")
    public String list(SearchParam searchParam,Model model){
        //调用查询方法
        Result<Map> result = this.listFeignClient.list(searchParam);
        //  urlParam  searchParam  trademarkParam  propsParamList  orderMap  需要自己组装
        //  trademarkList  attrsList  goodsList  pageNo  totalPages  这些数据都是实体类的属性  SearchResponseVo
        //  面包屑  trademarkParam  propsParamList
        //  品牌：品牌名称
        //  urlParam 表示点击平台属性值之前的url 路径
        String urlParam = this.makeUrlParam(searchParam);
        //  trademark=1:小米
        String trademarkParam = this.makeTradeMarkParam(searchParam.getTrademark());
        //  propsParamList 平台属性面包屑：平台属性名：平台属性值名  是多个！List<Param> map.get("attrName"):map.get("attrValue")
        List<SearchAttr> searchAttrList = this.makeSearchAttr(searchParam.getProps());
        //List<Map> mapList = this.makeSearchAttr(searchParam.getProps());

        Map<String, Object> orderMap = this.makeOrderMap(searchParam.getOrder());
        //  保存：trademarkList  attrsList  goodsList  pageNo  totalPages
        model.addAllAttributes(result.getData());
        model.addAttribute("searchParam", searchParam);
        model.addAttribute("urlParam", urlParam);
        model.addAttribute("trademarkParam",trademarkParam);
        model.addAttribute("propsParamList",searchAttrList);
        model.addAttribute("orderMap",orderMap);
        //返回视图名称；
        return "list/index";
    }

    /**
     * 制作排序
     * @param order
     * @return
     */
    private Map<String, Object> makeOrderMap(String order) {
        //  创建一个map集合
        Map<String, Object> map = new HashMap<>();
        if (!StringUtils.isEmpty(order)){
            String[] split = order.split(":");
            // 用户点击了排序
            if (split!=null&&split.length==2){
                //  按照哪种方式排序
                map.put("type", split[0]);
                //  设置排序规则 asc 或 desc
                map.put("sort", split[1]);
            }
        }else {
            //  按照哪种方式排序
            map.put("type", "1");
            //  设置排序规则 asc 或 desc
            map.put("sort","desc");
        }
        //返回集合
        return map;
    }

    /**
     * 平台属性面包屑集合
     * @param props
     * @return
     */
    private List<SearchAttr> makeSearchAttr(String[] props) {
        // 创建一个集合对象
        ArrayList<SearchAttr> searchAttrs = new ArrayList<>();
        // 判断
        if (props!=null && props.length>0){
            //props=24:256G:机身内存&props=23:8G:运行内存
            for (String prop : props) {
                // 分割
                String[] split = prop.split(":");
                if (split!=null&&split.length==3){
                    //  创建一个对象
                    SearchAttr searchAttr = new SearchAttr();
                    searchAttr.setAttrId(Long.parseLong(split[0]));
                    searchAttr.setAttrValue(split[1]);
                    searchAttr.setAttrName(split[2]);
                    searchAttrs.add(searchAttr);
                }
            }
        }
        return searchAttrs;
    }

    //  品牌面包屑
    private String makeTradeMarkParam(String trademark) {
        //trademark=1：小米
        if (!StringUtils.isEmpty(trademark)){
            String[] split = trademark.split(":");
            if (split!=null && split.length==2){
                return "品牌：" + split[1];
            }
        }
        return null;
    }

    /**
     * 记录用户通过哪些条件进行了检索
     * @param searchParam
     * @return
     */
    private String makeUrlParam(SearchParam searchParam) {
        // 字符串拼接
        /*
            @Override 安全：
            public synchronized StringBuffer append(String str) {
                toStringCache = null;
                super.append(str);
                return this;
            }
         */
        StringBuilder stringBuilder = new StringBuilder();
        //  判断用户根据什么文件进行了检索，有这个条件才拼接，没有拼接
        if (!StringUtils.isEmpty(searchParam.getKeyword())){
            //  http://list.gmall.com/list.html?keyword=手机
            stringBuilder.append("keyword=").append(searchParam.getKeyword());
        }

        //  判断用户是否根据分类Id 进行检索
        if (!StringUtils.isEmpty(searchParam.getCategory3Id())){
            //   http://list.gmall.com/list.html?category3Id=61
            stringBuilder.append("category3Id=").append(searchParam.getCategory3Id());
        }
        if (!StringUtils.isEmpty(searchParam.getCategory2Id())){
            //   http://list.gmall.com/list.html?category3Id=61
            stringBuilder.append("category2Id=").append(searchParam.getCategory2Id());
        }
        if (!StringUtils.isEmpty(searchParam.getCategory1Id())){
            //   http://list.gmall.com/list.html?category3Id=61
            stringBuilder.append("category1Id=").append(searchParam.getCategory1Id());
        }

        //  用户还可以根据品牌进行检索过滤
        //  http://list.gmall.com/list.html?keyword=手机&trademark=1:小米
        if (!StringUtils.isEmpty(searchParam.getTrademark())){
            if (stringBuilder.length()>0){
                stringBuilder.append("&trademark=").append(searchParam.getTrademark());
            }
        }
        //  还可以根据平台属性值过滤
        //  http://list.gmall.com/list.html?keyword=手机&trademark=1:小米&props=24:256G:机身内存&props=23:8G:运行内存
        String[] props = searchParam.getProps();
        if (props!=null && props.length>0){
            //循环遍历
            for (String prop : props) {
                if (stringBuilder.length()>0){
                    stringBuilder.append("&props=").append(prop);
                }
            }
        }
        //  返回用户的检索数据
        return "list.html?"+stringBuilder.toString();
    }
}
