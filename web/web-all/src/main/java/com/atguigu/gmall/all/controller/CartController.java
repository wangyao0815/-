package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.list.SearchAttr;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;

@Controller
public class CartController {

    @Autowired
    private ProductFeignClient productFeignClient;

    //  http://cart.gmall.com/addCart.html?skuId=28&skuNum=1&sourceType=query
    @GetMapping("addCart.html")
    public String addCart(HttpServletRequest request){
        String skuId = request.getParameter("skuId");
        //  获取skuInfo
        SkuInfo skuInfo = productFeignClient.getSkuInfo(Long.parseLong(skuId));
        //  保存数据
        request.setAttribute("skuInfo",skuInfo);
        request.setAttribute("skuNum", request.getParameter("skuNum"));
        //  返回添加成功界面
        return "cart/addCart";
    }

    //  查看购物车列表
    @GetMapping("/cart.html")
    public String cartList(){

        //  返回购物车列表页面
        return "cart/index";
    }
}
