package com.heima.behavior.service;

import com.heima.model.behavior.dtos.CollectionBehaviorDto;
import com.heima.model.common.dtos.ResponseResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;

public interface ApCollectionBehaviorService {
    public ResponseResult collectArticle(CollectionBehaviorDto dto);
}
