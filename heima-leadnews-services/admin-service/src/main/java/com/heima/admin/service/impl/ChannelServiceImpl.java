package com.heima.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.api.R;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.admin.mapper.ChannelMapper;
import com.heima.admin.service.ChannelService;
import com.heima.model.admin.dtos.ChannelDto;
import com.heima.model.admin.pojos.AdChannel;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service("adminService")
public class ChannelServiceImpl extends ServiceImpl<ChannelMapper, AdChannel> implements ChannelService {
    @Override
    public ResponseResult findPage(ChannelDto channelDto) {
        //1.判断参数及页码
        if (channelDto == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        channelDto.checkParam();
        //2.传入参数执行查询
        //分页参数
        IPage<AdChannel> page = new Page<>(channelDto.getPage(), channelDto.getSize());
        //编写查询条件
        LambdaQueryWrapper<AdChannel> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.isNotBlank(channelDto.getName())) {
            wrapper.like(AdChannel::getName, channelDto.getName());
        }
        //3.返回结果
        IPage<AdChannel> result = this.page(page, wrapper);
        List<AdChannel> records = result.getRecords();
        long total = result.getTotal();
        return new PageResponseResult(channelDto.getPage(), channelDto.getSize(), total, records);
    }

    @Override
    public ResponseResult add(AdChannel adChannel) {

        //1.判断参数
        if (adChannel == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID, "无数据");
        }
        //查询添加的名称是否存在
        int count = this.count(Wrappers.<AdChannel>lambdaQuery().eq(AdChannel::getName, adChannel.getName()));
        if (count > 0) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID, "名称已存在");
        }
        //2.创建添加语句
        adChannel.setCreatedTime(new Date());
        save(adChannel);
        //3.返回结果
        return ResponseResult.okResult();

    }

    @Override
    public ResponseResult update(AdChannel adChannel) {
        //1.判断参数
        if (adChannel == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID, "修改内容为空");
        }
        //2 执行修改
        AdChannel channel = getById(adChannel.getId());
        if (channel == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST, "频道信息不存在");
        }
        //3.校验名称唯一性
        //3. 校验名称唯一性
        if (StringUtils.isNotBlank(adChannel.getName())
                &&
                !adChannel.getName().equals(channel.getName())) {
            int count = this.count(Wrappers.<AdChannel>lambdaQuery()
                    .eq(AdChannel::getName, adChannel.getName()));
            if (count > 0) {
                return ResponseResult.errorResult(AppHttpCodeEnum.DATA_EXIST, "该频道已存在");
            }
        }
        //4.创建更新条件
        updateById(adChannel);
        //5.返回结果
        return ResponseResult.okResult();

    }

    @Override
    public ResponseResult delete(Integer id) {
        //1.判断参数
        if (id == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST, "id不存在");
        }
        AdChannel channel = getById(id);
        if (channel == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST, "此频道不存在");
        }
        if (channel.getStatus()) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_REQUIRE, "当前频道为启用状态，无法删除");
        }
        //2.创建删除条件语句
        removeById(id);
        //3.返回结果
        return ResponseResult.okResult();
    }
}
