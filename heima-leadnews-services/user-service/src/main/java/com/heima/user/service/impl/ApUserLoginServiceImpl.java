package com.heima.user.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.heima.common.exception.CustException;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.user.dtos.LoginDto;
import com.heima.model.user.pojos.ApUser;
import com.heima.user.mapper.ApUserMapper;
import com.heima.user.service.ApUserLoginService;
import com.heima.utils.common.AppJwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.HashMap;

@Service
@Slf4j
public class ApUserLoginServiceImpl implements ApUserLoginService {

    @Autowired
    ApUserMapper apUserMapper;

    @Override
    public ResponseResult login(LoginDto dto) {
        //1.校验参数
        if (dto == null) {
            CustException.cust(AppHttpCodeEnum.DATA_NOT_EXIST);
        }
        //2.业务实现
        //检查手机号和密码
        if (StringUtils.isNotBlank(dto.getPhone()) && StringUtils.isNotBlank(dto.getPassword())) {
            //根据手机号查询用户，进行校验
            ApUser apUser = apUserMapper.selectOne(Wrappers.<ApUser>lambdaQuery().eq(ApUser::getPhone, dto.getPhone()));
            if (apUser == null) {
                CustException.cust(AppHttpCodeEnum.DATA_NOT_EXIST, "此用户不存在");
            }
            String loginPwd = DigestUtils.md5DigestAsHex((dto.getPassword() + apUser.getSalt()).getBytes());
            if (!loginPwd.equals(apUser.getPassword())) {
                CustException.cust(AppHttpCodeEnum.LOGIN_PASSWORD_ERROR);
            }
            //检验状态
            if (apUser.getStatus().intValue() != 0) {
                CustException.cust(AppHttpCodeEnum.LOGIN_STATUS_ERROR);
            }
            //颁发token，封装返回结果user+token
            String token = AppJwtUtil.getToken(Long.valueOf(apUser.getId()));
            //新建map封装token及user数据，返回给前端
            HashMap map = new HashMap<>();
            map.put("token", token);
            apUser.setPassword("");
            apUser.setSalt("");
            map.put("user", apUser);
            return ResponseResult.okResult(map);
        } else {
            //检查设备id是否存在
            if (dto.getEquipmentId() == null) {
                CustException.cust(AppHttpCodeEnum.DATA_NOT_EXIST);
            }
            //直接给设备id返回一个token
            String token = AppJwtUtil.getToken(0L);
            HashMap map = new HashMap<>();
            map.put("token", token);
            return ResponseResult.okResult(map);

        }

    }
}
