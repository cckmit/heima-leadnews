package com.heima.behavior.controller;

import com.heima.behavior.service.ApLikeBehaviorService;
import com.heima.model.behavior.dtos.LikesBehaviorDto;
import com.heima.model.common.dtos.ResponseResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Api(value = "喜欢/不喜欢API",tags ="喜欢/不喜欢API" )
@RestController
@RequestMapping
public class ApLikesBehaviorController {

    @Autowired
    ApLikeBehaviorService apLikeBehaviorService;

    @PostMapping("/api/v1/likes_behavior")
    @ApiOperation(value = "喜欢/不喜欢请求",tags ="喜欢/不喜欢请求" )
    public ResponseResult likeOrUnlike(@RequestBody @Validated LikesBehaviorDto likesBehaviorDto){
        // likeOrUnlikeservice
        return apLikeBehaviorService.likeOrUnlike(likesBehaviorDto);
    }
}
