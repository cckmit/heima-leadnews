package com.heima.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.common.exception.CustException;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.user.dtos.AuthDto;
import com.heima.model.user.pojos.ApUserRealname;
import com.heima.user.mapper.ApUserRealnameMapper;
import com.heima.user.service.ApUserRealnameService;
import org.springframework.stereotype.Service;

@Service("apUserRealnameService")
public class ApUserRealnameServiceImpl extends ServiceImpl<ApUserRealnameMapper, ApUserRealname> implements ApUserRealnameService {
    @Override
    public ResponseResult loadListByStatus(AuthDto dto) {
        //1.校验参数
        if (dto==null) {
            CustException.cust(AppHttpCodeEnum.PARAM_INVALID);
        }
        dto.checkParam();
        //2.创造查询条件
        Page<ApUserRealname> page=new Page<>(dto.getPage(),dto.getSize());
        //页面中有对状态进行分组，所有需要添加查询条件

        LambdaQueryWrapper<ApUserRealname> listWrapper = Wrappers.<ApUserRealname>lambdaQuery().eq(dto.getStatus()!=null,ApUserRealname::getStatus, dto.getStatus());
        IPage<ApUserRealname> pageResult = page(page, listWrapper);
        //3.返回结果
        return new PageResponseResult(dto.getPage(),dto.getSize(),pageResult.getTotal(),pageResult.getRecords());
    }
}
