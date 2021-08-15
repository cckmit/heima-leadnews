package com.heima.behavior.controller;

import com.heima.behavior.service.ApUnlikesBehaviorService;

import com.heima.model.behavior.dtos.UnLikesBehaviorDto;
import com.heima.model.common.dtos.ResponseResult;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
@Api(value ="不喜欢/取消不喜欢API" ,tags ="不喜欢/取消不喜欢API" )
public class ApUnlikesBehaviorController {

    @Autowired
    ApUnlikesBehaviorService apUnlikesBehaviorService;

    @PostMapping("/api/v1/un_likes_behavior/")
    public ResponseResult apUnlikesBehavior(@RequestBody @Validated UnLikesBehaviorDto unLikesBehaviorDto){
        return apUnlikesBehaviorService.apUnlikesBehavior(unLikesBehaviorDto);
    }


}
