package com.heima.admin.service;


import com.heima.model.admin.dtos.ChannelDto;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;

public interface ChannelService {
    ResponseResult findPage(ChannelDto channelDto);
}
