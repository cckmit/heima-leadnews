package com.heima.behavior.controller;

import com.heima.behavior.service.ApCollectionBehaviorService;
import com.heima.model.behavior.dtos.CollectionBehaviorDto;
import com.heima.model.common.dtos.ResponseResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Api(value = "收藏行为API",tags = "收藏行为API")
@RestController
@RequestMapping
public class ApCollectionBehaviorController {
    @Autowired
    ApCollectionBehaviorService apCollectionBehaviorService;

    @ApiOperation("保存或取消 收藏行为")
    @PostMapping("api/v1/collection_behavior/")
    public ResponseResult collectArticle(@RequestBody @Validated CollectionBehaviorDto dto){
        return apCollectionBehaviorService.collectArticle(dto);
    }
}
