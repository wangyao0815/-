package com.atguigu.gmall.user.service.impl;

import com.atguigu.gmall.common.util.MD5;
import com.atguigu.gmall.model.user.UserInfo;
import com.atguigu.gmall.user.mapper.UserInfoMapper;
import com.atguigu.gmall.user.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

@Service
public class UserServiceImpl implements UserService {

    //  注入mapper 层
    @Autowired
    private UserInfoMapper userInfoMapper;

    @Override
    public UserInfo login(UserInfo userInfo) {
        //  查询数据库
        //  select * from userInfo where uname = ? and pwd = ?;
        QueryWrapper<UserInfo> userInfoQueryWrapper = new QueryWrapper<>();
        userInfoQueryWrapper.eq("login_name", userInfo.getLoginName());
        String passwd = userInfo.getPasswd();
        String newPwd = MD5.encrypt(passwd); //        DigestUtils.md5DigestAsHex(passwd.getBytes());
        userInfoQueryWrapper.eq("passwd", newPwd);
        UserInfo info = userInfoMapper.selectOne(userInfoQueryWrapper);
        //判断
        if (info != null) {
            return info;
        }
        return null;
    }
}
