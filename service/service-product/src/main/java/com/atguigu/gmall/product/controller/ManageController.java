package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.BaseCategory1;
import org.apache.catalina.manager.ManagerServlet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("admin/product/")//表示在当前这个类中，有很多映射路径，所有的映射路径都是以 admin/product/
public class ManageController {

    @Autowired
    private ManagerService managerService;

    //http://localhost/admin/product/getCategory1
    @GetMapping("getCategory1")
    public Result getCategory1(){
        //调用服务层
        List<BaseCategory1> baseCategory1List = managerService.getCategory1();
        return Result.ok(baseCategory1List);
    }
}
