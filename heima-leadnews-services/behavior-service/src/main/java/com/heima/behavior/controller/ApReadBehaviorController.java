package com.heima.behavior.controller;

import com.heima.behavior.service.ApReadBehaviorService;
import com.heima.model.behavior.dtos.ReadBehaviorDto;
import com.heima.model.common.dtos.ResponseResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author limingfei
 */
@RestController
@RequestMapping
@Api(value = "阅读行为API",tags ="阅读行为API" )
public class ApReadBehaviorController {

    @Autowired
    ApReadBehaviorService apReadBehaviorService;

    @PostMapping("/api/v1/read_behavior")
    @ApiOperation(value = "阅读行为request",tags ="阅读行为request" )
    public ResponseResult apReadBehavior(@RequestBody @Validated ReadBehaviorDto readBehaviorDto){
        return apReadBehaviorService.apReadBehavior(readBehaviorDto);
    }
}
