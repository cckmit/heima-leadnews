package com.heima.wemedia.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.pojos.WmUser;
import com.heima.wemedia.mapper.WemediaMapper;
import com.heima.wemedia.service.WemediaServie;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service("WemediaService")
@Slf4j
public class WemediaServiceImpl extends ServiceImpl<WemediaMapper, WmUser> implements WemediaServie {
    @Override
    public ResponseResult<WmUser> findByName(String name) {
        WmUser wmUser = getOne(Wrappers.<WmUser>lambdaQuery().eq(WmUser::getName, name));
        return ResponseResult.okResult(wmUser);
    }
}
