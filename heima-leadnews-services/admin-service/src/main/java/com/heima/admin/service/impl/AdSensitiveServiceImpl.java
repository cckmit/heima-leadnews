package com.heima.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.admin.mapper.AdSensitiveMapper;
import com.heima.admin.service.AdSensitiveService;
import com.heima.common.exception.CustException;
import com.heima.model.admin.dtos.SensitiveDto;
import com.heima.model.admin.pojos.AdSensitive;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("adSensitiveService")
@Slf4j
@Transactional(rollbackFor = Exception.class)
public class AdSensitiveServiceImpl extends ServiceImpl<AdSensitiveMapper, AdSensitive> implements AdSensitiveService {
    @Override
    public ResponseResult list(SensitiveDto dto) {
        //1.参数校验
        if (dto == null) {
            CustException.cust(AppHttpCodeEnum.PARAM_REQUIRE, "敏感词为空");
        }
        dto.checkParam();
        //分页
        IPage<AdSensitive> page = new Page<>(dto.getPage(), dto.getSize());
        //2.执行查询，需要进行名字是否为空的判断，如果名字是空，就不要建立查询条件
        //      是空建立的条件照名字查出来都是null
        //IPage<AdSensitive> pageResult=page(page,queryWrapper);
        //queryWrapper为null时，pageResult是默认查询page大小的数据出来
        LambdaQueryWrapper<AdSensitive> queryWrapper = Wrappers.<AdSensitive>lambdaQuery().like(StringUtils.isNoneBlank(dto.getName()), AdSensitive::getSensitives, dto.getName());
        IPage<AdSensitive> pageResult = page(page, queryWrapper);
        //3.返回结果
        ResponseResult ResponseResult = new PageResponseResult(dto.getPage(), dto.getSize(), pageResult.getTotal());
        ResponseResult.setData(pageResult.getRecords());
        return ResponseResult;
    }

    @Override
    public ResponseResult insert(AdSensitive adSensitive) {
        //1.校验参数
        if (adSensitive == null || !StringUtils.isNotBlank(adSensitive.getSensitives())) {
            CustException.cust(AppHttpCodeEnum.PARAM_INVALID);
        }
        //2.创建增加条件
        //先确认此敏感词数据库中是否存在
        int count = this.count(Wrappers.<AdSensitive>lambdaQuery().eq(AdSensitive::getSensitives, adSensitive.getSensitives()));
        if (count > 0) {
            CustException.cust(AppHttpCodeEnum.DATA_EXIST, "敏感词已存在");
        }
        //3.返回结果
        save(adSensitive);
        return ResponseResult.okResult();
    }

    @Override
    public ResponseResult update(AdSensitive adSensitive) {
        //1.校验参数
        if (adSensitive == null || !StringUtils.isNotBlank(adSensitive.getSensitives())) {
            CustException.cust(AppHttpCodeEnum.PARAM_INVALID);
        }
        //2.创建修改条件
        int count = this.count(Wrappers.<AdSensitive>lambdaQuery().eq(AdSensitive::getSensitives, adSensitive.getSensitives()));
        if (count > 0) {
            CustException.cust(AppHttpCodeEnum.DATA_EXIST, "修改的敏感词已存在");
        }
        //3.返回结果
        updateById(adSensitive);
        return ResponseResult.okResult();
    }

    @Override
    public ResponseResult delete(Integer id) {
        //1.校验参数
        if (id == null) {
            CustException.cust(AppHttpCodeEnum.DATA_NOT_EXIST, "敏感词不能为空");
        }
        //2.创造删除条件
        removeById(id);
        //3.返回结果

        return ResponseResult.okResult();
    }
}
