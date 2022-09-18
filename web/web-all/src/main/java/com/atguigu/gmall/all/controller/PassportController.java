package com.atguigu.gmall.all.controller;

import lombok.SneakyThrows;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;

@Controller
public class PassportController {

    //  http://passport.gmall.com/login.html?originUrl=http://item.gmall.com/26.html、
    //  从添加购物车的时候跳转
    //  http://passport.gmall.com/login.html?originUrl=http://cart.gmall.com/addCart.html?skuId=28&skuNum=1
    //  http://cart.gmall.com/addCart.html?skuId=28&skuNum=1
    @SneakyThrows
    @GetMapping("login.html")
    public String login(HttpServletRequest request) {
        String originUrl = request.getParameter("originUrl");
        //  request.setAttribute("originUrl",originUrl);
        //  http://cart.gmall.com/addCart.html?skuId=28
        //  System.out.println(request.getRequestURI());
        //  String encode = URLEncoder.encode(originUrl, "UTF-8");
        //  System.out.println(encode);
        //  System.out.println("originUrl:\t"+originUrl);
        //  ${originUrl}
        String queryString = request.getQueryString(); // originUrl=http://cart.gmall.com/addCart.html?skuId=28&skuNum=1
        request.setAttribute("originUrl", queryString.substring(queryString.indexOf("=") + 1));
        //  返回登录页面.
        return "login";
    }
}
