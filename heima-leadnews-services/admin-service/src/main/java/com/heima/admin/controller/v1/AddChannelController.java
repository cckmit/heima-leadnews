package com.heima.admin.controller.v1;

import com.heima.admin.service.ChannelService;
import com.heima.model.admin.dtos.ChannelDto;
import com.heima.model.admin.pojos.AdChannel;
import com.heima.model.common.dtos.ResponseResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/channel")
@Api(value = "频道管理", tags = "频道管理", description = "频道管理API")
public class AddChannelController {

    @Autowired
    private ChannelService channelService;

    @PostMapping("/list")
    @ApiOperation("频道分页列表查询")
    public ResponseResult list(@RequestBody ChannelDto channelDto){
        return channelService.findPage(channelDto);
    }

    @PostMapping("/save")
    @ApiOperation("添加新的频道")
    public ResponseResult save(@RequestBody AdChannel adChannel){
        return channelService.add(adChannel);
    }
}
