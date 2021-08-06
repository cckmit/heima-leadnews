package com.heima.wemedia.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.common.exception.CustException;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.wemedia.dtos.WmUserDto;
import com.heima.model.wemedia.pojos.WmUser;
import com.heima.model.wemedia.vos.WmUserVO;
import com.heima.utils.common.AppJwtUtil;
import com.heima.wemedia.mapper.WmUserMapper;
import com.heima.wemedia.service.WmUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.HashMap;

/**
 * 登录逻辑
 */
@Service("wmUserService")
@Slf4j
public class WmUserServiceImpl extends ServiceImpl<WmUserMapper, WmUser> implements WmUserService {
    @Override
    public ResponseResult login(WmUserDto dto) {
        //1.校验参数  hibernate validated校验
        //2.创造条件
        //根据dto查询wmuser表内是否有这个用户
        WmUser wmUser = getOne(Wrappers.<WmUser>lambdaQuery().eq(WmUser::getName, dto.getName()));
        if (wmUser==null) {
            CustException.cust(AppHttpCodeEnum.DATA_NOT_EXIST,"无此用户");
        }
        if (wmUser.getStatus().intValue()!=9) {
            CustException.cust(AppHttpCodeEnum.LOGIN_STATUS_ERROR);
        }
        //有这个用户以后，再进行密码的确认
        String pswd = DigestUtils.md5DigestAsHex((dto.getPassword() + wmUser.getSalt()).getBytes());
        if (!wmUser.getPassword().equals(pswd)) {
            CustException.cust(AppHttpCodeEnum.LOGIN_PASSWORD_ERROR);
        }
        //3.返回jwt结果
        HashMap<String, Object> map = new HashMap<>();
        map.put("token", AppJwtUtil.getToken(Long.valueOf(wmUser.getId())));
        WmUserVO wmUserVO = new WmUserVO();
        BeanUtils.copyProperties(wmUser,wmUserVO);
        map.put("user",wmUserVO);
        return ResponseResult.okResult(map);
    }
}
