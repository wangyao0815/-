package com.atguigu.gmall.cart.controller;

import com.atguigu.gmall.cart.service.CartService;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.util.AuthContextHolder;
import com.atguigu.gmall.model.cart.CartInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.sql.rowset.CachedRowSet;
import java.util.List;

@RestController
@RequestMapping("api/cart")
public class CartApiController {

    @Autowired
    private CartService cartService;

    //  添加购物车
    @GetMapping("addToCart/{skuId}/{skuNum}")
    public Result addToCart(@PathVariable Long skuId,
                            @PathVariable Integer skuNum,
                            HttpServletRequest request){
        //  获取用户Id
        String userId = AuthContextHolder.getUserId(request);
        //  判断
        if (StringUtils.isEmpty(userId)){
            //  获取一个临时用户id
            userId = AuthContextHolder.getUserTempId(request);
        }
        //  调用服务层方法
        cartService.addToCart(skuId,userId,skuNum);
        return Result.ok();
    }

    //  查看购物车列表
    @GetMapping("cartList")
    public Result getCartList(HttpServletRequest request){
        //  获取用户Id
        String userId = AuthContextHolder.getUserId(request);
        //  获取一个临时用户id
        String userTempId = AuthContextHolder.getUserTempId(request);
        //  调用服务层方法
        List<CartInfo> cartInfoList = this.cartService.getCartList(userId,userTempId);
        //  返回数据
        return Result.ok(cartInfoList);
    }

    //  选中状态
    @GetMapping("checkCart/{skuId}/{isChecked}")
    public Result CheckCart(@PathVariable Long skuId,
                            @PathVariable Integer isChecked,
                            HttpServletRequest request){
        //  获取用户Id
        String userId = AuthContextHolder.getUserId(request);
        //  判断
        if (StringUtils.isEmpty(userId)){
            //  获取一个临时用户id
            userId = AuthContextHolder.getUserTempId(request);
        }
        //  调用服务层方法
        this.cartService.CheckCart(skuId,userId,isChecked);
        //  返回数据
        return Result.ok();
    }
    //  删除购物项
    //  hdel key field;
    @DeleteMapping("/deleteCart/{skuId}")
    public Result deleteCart(@PathVariable Long skuId,
                             HttpServletRequest request){
        //  获取用户Id
        String userId = AuthContextHolder.getUserId(request);
        //  判断
        if (StringUtils.isEmpty(userId)){
            //  获取一个临时用户id
            userId = AuthContextHolder.getUserTempId(request);
        }
        //  调用服务层方法
        this.cartService.deleteCart(skuId,userId);
        //  返回
        return Result.ok();
    }

    //  /api/cart/getCartCheckedList/{userId}
    @GetMapping("getCartCheckedList/{userId}")
    public List<CartInfo> getCartCheckedList(@PathVariable String userId){
        //  调用服务层方法
        return this.cartService.getCartCheckedList(userId);
    }
}
