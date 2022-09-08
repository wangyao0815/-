package com.atguigu.gmall.gateway.filter;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.result.ResultCodeEnum;
import com.atguigu.gmall.common.util.IpUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class AuthGlobalFilter implements GlobalFilter {
    /*
        authUrls:
            url: trade.html,myOrder.html,list.html
    */
    @Value("${authUrls.url}")
    private String authUrls; //authUrls = trade.html,myOrder.html,list.html

    @Autowired
    private RedisTemplate redisTemplate;

    private AntPathMatcher pathMatcher = new AntPathMatcher();
    /**
     *
     * @param exchange  spring框架封装的web服务请求 request与响应 response 对象
     * @param chain   过滤器对象
     * @return
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        //  先获取到url 路径
        ServerHttpRequest request = exchange.getRequest();
        //  request.getURI()    // http://localhost/api/product/inner/getSkuInfo/28
        String path = request.getURI().getPath();
        //  判断是否属于内部数据接口
        if (pathMatcher.match("/**/inner/**", path)){
            //  设置响应
            ServerHttpResponse response = exchange.getResponse();
            //  提示用户没有权限访问这样的路径
            return out(response, ResultCodeEnum.PERMISSION);
        }

        //  获取到登录的userId，在缓存中获取数据，必须要有token,token存储在header 或 cookie
        String userId = this.getUserId(request);
        //  判断是否属于非法登录 redisIp != ip  return=-1;
        if ("-1".equals(userId)){
            //  设置响应
            ServerHttpResponse response = exchange.getResponse();
            //  提示用户没有权限访问这样的路径
            return out(response, ResultCodeEnum.PERMISSION);
        }

        //  /api/**/auth/**  必须要登录
        if (pathMatcher.match("/api/**/auth/**", path)){
            //  判断用户是否登录！如果未登录，则提示信息！
            if (StringUtils.isEmpty(userId)){
                //  提示信息
                ServerHttpResponse response = exchange.getResponse();
                //  提示用户未登录
                return out(response, ResultCodeEnum.LOGIN_AUTH);
            }
        }

        //  用户在访问哪些业务层控制器时，需要登录，我的订单，添加购物车都拦截！需要将哪些控制器添加到配置文件中，做软编码
        //  authUrls = trade.html,myOrder.html,list.html
        //  http://list.qmall.com/list.html?category3Id=61
        String[] split = authUrls.split(",");
        if (split!=null && split.length>0){
            for (String url : split) {
                //  path 包含上述控制器 并且 用户Id为空，此时需要拦截并跳转
                if (path.indexOf(url)!=-1 && StringUtils.isEmpty(userId)){
                    //  获取响应
                    ServerHttpResponse response = exchange.getResponse();
                    //  设置一些参数
                    response.setStatusCode(HttpStatus.SEE_OTHER);
                    //  重定向到登录页面   http://passport.gmall.com/login.html?originUrl=http://www.gmall.com/
                    response.getHeaders().set(HttpHeaders.LOCATION,"http://passport.gmall.com/login.html?originUrl="+request.getURI());
                    //  重定向
                    return response.setComplete();
                }
            }
        }

        //  将获取到的用户Id 添加到请求头：请求头可能会存有userId  OOP思想
        //  以后使用这个类AuthContextHolder 来获取用户Id
        if (!StringUtils.isEmpty(userId)){
            //  放入请求头：
            request.mutate().header("userId", userId).build();
            //  exchange  与 request 关联起来了
            return chain.filter(exchange.mutate().request(request).build());
        }
        //  默认返回，表示这个过滤器结束了
        return chain.filter(exchange);
    }

    /**
     * 获取用户id
     * @param request
     * @return
     */
    private String getUserId(ServerHttpRequest request) {
        //  声明一个token
        String token = "";
        //  用户Id 可能存储在cookie 或 header
        HttpCookie httpCookie = request.getCookies().getFirst("token");
        if (httpCookie!=null){
            token = httpCookie.getValue();
        }else {
        //  从header 中获取！
            List<String> stringList = request.getHeaders().get("token");
            if (!CollectionUtils.isEmpty(stringList)){
                token=stringList.get(0);
            }
        }

        //  判断token 不为空
        if (!StringUtils.isEmpty(token)){
            //  组成缓存key
            String userLoginKey = "user:login:"+token;
            //  从缓存中获取userId
            String strJson = (String) this.redisTemplate.opsForValue().get(userLoginKey);
            if (!StringUtils.isEmpty(strJson)){
                JSONObject jsonObject = JSON.parseObject(strJson);
                //  判断当前缓存中的ip 与 正在操作的客户端ip 地址是否一致！
                String ip = (String) jsonObject.get("ip");
                //  如果ip地址相同则返回用户Id
                if (IpUtil.getGatwayIpAddress(request).equals(ip)){
                    //  获取userId
                    String userId = (String) jsonObject.get("userId");
                    return userId;
                }else {
                 return "-1";
                }
            }
        }
        //  默认返回""
        return "";
    }


        /**
         * 输出方法
         * @param response
         * @param resultCodeEnum
         * @return
         */
    private Mono<Void> out(ServerHttpResponse response,ResultCodeEnum resultCodeEnum){
        //  输出的内容：没有权限，未登录  细节处理：设置每个页面的   Headers: Content-Type=application/json
        //  String message = resultCodeEnum.getMessage();
        Result result = Result.build(null, resultCodeEnum);
        //  将这个result 变为String
        String str = JSON.toJSONString(result);
        //  产生DateBuffer
        DataBuffer wrap = response.bufferFactory().wrap(str.getBytes());
        //  设置请求头类型
        response.getHeaders().add("Content-Type", "application/json;charset=UTF-8");
        //  输出数据
        return response.writeWith(Mono.just(wrap));
    }
}
