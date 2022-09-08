package com.atguigu.gmall.all.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;

@Controller
public class PassportController {

    //
    @GetMapping("login.html")
    public String login(HttpServletRequest request){

        request.setAttribute("originUrl", request.getParameter("originUrl"));

        //返回登录界面
        return "login";
    }
}
