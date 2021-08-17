package com.heima.comment.service;

import com.heima.model.comment.dtos.CommentDto;
import com.heima.model.comment.dtos.CommentLikeDto;
import com.heima.model.comment.dtos.CommentSaveDto;
import com.heima.model.common.dtos.ResponseResult;


public interface CommentService {
    public ResponseResult saveComment( CommentSaveDto dto);

    /**
     * 点赞评论
     * @param dto
     * @return
     */
    public ResponseResult like(CommentLikeDto dto);

    /**
     * 根据文章id查询评论列表
     * @return
     */
    public ResponseResult findByArticleId(CommentDto dto);
}
