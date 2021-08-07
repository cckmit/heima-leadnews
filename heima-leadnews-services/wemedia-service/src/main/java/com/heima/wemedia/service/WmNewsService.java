package com.heima.wemedia.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.dtos.WmNewsDto;
import com.heima.model.wemedia.dtos.WmNewsPageReqDto;
import com.heima.model.wemedia.pojos.WmNews;

public interface WmNewsService extends IService<WmNews> {

    /**
     * 查询所有自媒体文章
     * @return
     */
    public ResponseResult findList(WmNewsPageReqDto dto);

    /**
     * 自媒体文章发布
     * @param wmNewsDto
     * @return
     */
    ResponseResult submitNews(WmNewsDto wmNewsDto);
}
