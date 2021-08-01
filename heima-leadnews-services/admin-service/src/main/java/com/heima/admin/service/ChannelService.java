package com.heima.admin.service;


import com.heima.model.admin.dtos.ChannelDto;
import com.heima.model.admin.pojos.AdChannel;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;

public interface ChannelService {
    ResponseResult findPage(ChannelDto channelDto);

    ResponseResult add(AdChannel adChannel);

    ResponseResult update(AdChannel adChannel);

    ResponseResult delete(Integer id);
}
