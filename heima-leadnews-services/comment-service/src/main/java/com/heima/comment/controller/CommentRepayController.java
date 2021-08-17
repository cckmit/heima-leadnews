package com.heima.comment.controller;


import com.heima.comment.service.CommentRepayService;
import com.heima.model.comment.dtos.CommentRepayDto;
import com.heima.model.comment.dtos.CommentRepayLikeDto;
import com.heima.model.comment.dtos.CommentRepaySaveDto;
import com.heima.model.common.dtos.ResponseResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Api(value = "评论回复API",tags = "评论回复API")
@RequestMapping("/api/v1/comment_repay")
public class CommentRepayController {

    @Autowired
    private CommentRepayService commentRepayService;

    @PostMapping("/load")
    @ApiOperation("加赞评论回复列表")
    public ResponseResult loadCommentRepay(@RequestBody CommentRepayDto dto){
    return commentRepayService.loadCommentRepay(dto);
    }

    @PostMapping("/save")
    @ApiOperation("保存评论回复")
    public ResponseResult saveCommentRepay(@RequestBody CommentRepaySaveDto dto){
        return commentRepayService.saveCommentRepay(dto);
    }

    @PostMapping("/like")
    @ApiOperation("保存回复评论点赞")
    public ResponseResult saveCommentRepayLike(
      @RequestBody CommentRepayLikeDto dto){
        return commentRepayService.saveCommentRepayLike(dto);
    }

}
