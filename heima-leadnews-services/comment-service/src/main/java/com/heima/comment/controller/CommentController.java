package com.heima.comment.controller;

import com.heima.comment.service.CommentService;
import com.heima.model.comment.dtos.CommentDto;
import com.heima.model.comment.dtos.CommentLikeDto;
import com.heima.model.comment.dtos.CommentSaveDto;
import com.heima.model.common.dtos.ResponseResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Api(value = "评论管理API",tags = "评论管理API")
@RestController
@RequestMapping("/api/v1/comment")
public class CommentController {

    @Autowired
    CommentService commentService;

  @ApiOperation("保存评论信息")
  @PostMapping("/save")
  public ResponseResult saveComment(@RequestBody CommentSaveDto dto){
		//  to be continue
      return commentService.saveComment(dto);
  }

    @ApiOperation("保存评论点赞")
    @PostMapping("/like")
    public ResponseResult like(@RequestBody CommentLikeDto dto){
        // TODO ===========业务实现
        return commentService.like(dto);
    }

    /**
     * 查询评论
     * @param dto
     * @return
     */
    @ApiOperation("展示评论列表")
    @PostMapping("/load")
    public ResponseResult findByArticleId(@RequestBody CommentDto dto){
        // ======= TODO 业务实现
        return commentService.findByArticleId(dto);
    }
}
