package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.context.IContext;

import java.io.FileWriter;
import java.io.IOException;

@Controller
public class IndexController {

    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private TemplateEngine templateEngine;

    //访问首页控制器
    // www.gmall.com www.gmall.com/index.html
    @GetMapping({"index.html","/"})
    public String index(Model model){
        //调用数据
        Result result = productFeignClient.getBaseCategoryList();
        model.addAttribute("list", result.getData());
        //存储list
        return "index/index";
    }

    //  创建静态化页面
    @GetMapping("createIndex")
    @ResponseBody
    public Result createIndex(){
        //  定义输出对象
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter("D:\\index.html");
        } catch (IOException e) {
            e.printStackTrace();
        }
        //  设置页面要显示的内容
        Result result = productFeignClient.getBaseCategoryList();
        Context context = new Context();
        context.setVariable("list", result.getData());

        //    void process(String var1, IContext var2, Writer var3);
        templateEngine.process("index/index.html", context,fileWriter);
        //  返回数据
        return Result.ok();
    }
}
