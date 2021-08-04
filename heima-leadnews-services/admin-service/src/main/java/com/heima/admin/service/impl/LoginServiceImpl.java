package com.heima.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.admin.mapper.LoginMapper;
import com.heima.admin.service.LoginService;
import com.heima.common.exception.CustException;
import com.heima.model.admin.dtos.AdUserDto;
import com.heima.model.admin.pojos.AdUser;
import com.heima.model.admin.vo.AdUserVO;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.utils.common.AppJwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.*;

@Service("loginService")
@Slf4j
public class LoginServiceImpl extends ServiceImpl<LoginMapper, AdUser> implements LoginService {
    @Override
    public ResponseResult logIn(AdUserDto userDto) {
        //1.校验参数
        if (userDto == null) {
            CustException.cust(AppHttpCodeEnum.PARAM_INVALID, "登录信息不能为空");
        }
        if (!StringUtils.isNotBlank(userDto.getName())) {
            CustException.cust(AppHttpCodeEnum.PARAM_INVALID, "用户名不能为空");
        }
        if (!StringUtils.isNotBlank(userDto.getPassword())) {
            CustException.cust(AppHttpCodeEnum.PARAM_INVALID, "密码不能为空");
        }
        //2.创建查询条件进行数据库校验
        LambdaQueryWrapper<AdUser> loginWrapper = Wrappers.<AdUser>lambdaQuery().eq(AdUser::getName, userDto.getName());
        AdUser user = getOne(loginWrapper);
        //根据传来的name查询用户是否存在
        if (user == null) {
            CustException.cust(AppHttpCodeEnum.DATA_NOT_EXIST, "用户不存在");
        }
        //根据查出来的用户确认用户的密码与传来的密码是否一致
        String salt = user.getSalt();
        //如果salt是null，将salt定义为""
        if (user.getSalt() == null) {
            salt = "";
        }
        String loginPwd = userDto.getPassword() + salt;
        String userPwd = user.getPassword() + salt;
        if (!loginPwd.equals(userPwd)) {
            CustException.cust(AppHttpCodeEnum.LOGIN_PASSWORD_ERROR);
        }
        // 4. 判断用户状态是否为 9 (正常)
        if (user.getStatus().intValue() != 9) {
            CustException.cust(AppHttpCodeEnum.DATA_NOT_ALLOW, "用户状态异常");
        }
        // 5. 更新最后登录时间
        user.setLoginTime(new Date());
        updateById(user);
        // 6. 生成token 封装返回结果   {user: {用户信息}  ,  token: "令牌凭证" }
        String token = AppJwtUtil.getToken(Long.valueOf(user.getId()));
        AdUserVO adUserVO = new AdUserVO();
        BeanUtils.copyProperties(user, adUserVO);
        Map map = new HashMap<>();
        map.put("token", token);
        map.put("user", adUserVO);
        //3.返回结果
        return ResponseResult.okResult(map);

    }

    @Override
    public ResponseResult register(AdUser adUser) {
        //1.校验参数
        if (adUser == null) {
            CustException.cust(AppHttpCodeEnum.DATA_NOT_EXIST, "注册信息为空");
        }
        //检查注册的用户是否存在
        int count = count(Wrappers.<AdUser>lambdaQuery().eq(AdUser::getName, adUser.getName()).eq(AdUser::getPassword, adUser.getPassword()).eq(AdUser::getEmail, adUser.getPassword()));
        if (count > 0) {
            CustException.cust(AppHttpCodeEnum.DATA_EXIST, "当前用户已经注册");
        }
        //2.创造保存条件
        adUser.setStatus(9);
        //3.返回结果
        save(adUser);
        return ResponseResult.okResult();

    }
}
