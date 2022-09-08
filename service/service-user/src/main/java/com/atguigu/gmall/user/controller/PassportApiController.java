package com.atguigu.gmall.user.controller;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.util.IpUtil;
import com.atguigu.gmall.model.user.UserInfo;
import com.atguigu.gmall.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import org.springframework.web.bind.annotation.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/user/passport")
public class PassportApiController {

    @Autowired
    private UserService userService;

    @Autowired
    private RedisTemplate redisTemplate;

    ///api/user/passport/login
    @PostMapping("/login")
    public Result login(@RequestBody UserInfo userInfo, HttpServletRequest request){
        UserInfo info = userService.login(userInfo);
        // 调用登录方法
        if (info!=null){
            //  生成一个 token
            String token = UUID.randomUUID().toString();
            HashMap<String, Object> hashMap = new HashMap<>();
            hashMap.put("token",token);
            //  登录成功之后，应该在页面回显用户昵称！ info.getNickName() cookie中
            hashMap.put("nickName", info.getNickName());
            //  为了完成单点登录业务处理，在此应该将用户关键信息写入缓存！以后判断用户是否登录或获取用户等数据直接从缓存中获取即可
            //  类型  存储什么？ userId!
            //  userLoginKey = user:login:token;
            String userLoginKey = RedisConst.USER_LOGIN_KEY_PREFIX+token;
            //  添加一个ip地址，防止token 被盗用；
            String ip = IpUtil.getIpAddress(request);
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("userId", info.getId().toString());
            jsonObject.put("ip", ip);
            this.redisTemplate.opsForValue().set(userLoginKey,jsonObject.toString(),RedisConst.USERKEY_TIMEOUT, TimeUnit.SECONDS);
            //  返回map 集合
            return Result.ok(hashMap);
        }else {
            return Result.fail().message("登录失败，请联系管理员");
        }
    }
    // 退出登录
    @GetMapping("logout")
    public Result logout(HttpServletRequest request,@RequestHeader String token){
        //  js 中已经将cookie 数据进行了清空，现在应该清空缓存数据

        //  userLoginKey = user:login:token;
        //  token 从哪里获取？ cookie -- 有
        //  String token = "";
        //        Cookie[] cookies = request.getCookies();
        //        //  判断
        //        if (cookies!=null && cookies.length>0){
        //            //  遍历
        //            for (Cookie cookie : cookies) {
        //                String name = cookie.getName();
        //                if ("token".equals(name)){
        //                    token = cookie.getValue();
        //                }
        //            }
        //        }

        //  异步请求的时候，拦截器，将token中的数据存储到header中
        //  token = request.getHeader("token");
        String userLoginKey = RedisConst.USER_LOGIN_KEY_PREFIX+token;
        //  删除缓存key
        this.redisTemplate.delete(userLoginKey);

        return Result.ok();
    }
}
