package com.heima.wemedia.controller.v1;

import com.heima.common.constants.wemedia.WemediaConstants;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.dtos.NewsAuthDto;
import com.heima.model.wemedia.dtos.WmNewsDto;
import com.heima.model.wemedia.dtos.WmNewsPageReqDto;
import com.heima.model.wemedia.pojos.WmNews;
import com.heima.wemedia.mapper.WmNewsMapper;
import com.heima.wemedia.service.WmNewsService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(value = "自媒体文章管理API", tags = "自媒体文章管理API")
@RestController
@RequestMapping("/api/v1/news")
public class WmNewsController {
    @Autowired
    private WmNewsService wmNewsService;


    @ApiOperation("根据条件查询文章列表")
    @PostMapping("/list")
    public ResponseResult findAll(@RequestBody WmNewsPageReqDto wmNewsPageReqDto) {
        return wmNewsService.findList(wmNewsPageReqDto);
    }

    @ApiOperation(value = "发表文章", notes = "发表文章，保存草稿，修改文章 共用的方法")
    @PostMapping("/submit")
    public ResponseResult submitNews(@RequestBody WmNewsDto wmNews) {
        return wmNewsService.submitNews(wmNews);
    }

    @GetMapping("/one/{id}")
    public ResponseResult findWmNewsById(@PathVariable("id") Integer id) {

        return wmNewsService.findWmNewsById(id);
    }

    @ApiOperation(value = "删除文章", notes = "删除文章")
    @GetMapping("/del_news/{id}")
    public ResponseResult delNews(@PathVariable("id") Integer id) {
        return wmNewsService.delNews(id);
    }

    @ApiOperation(value = "自媒体文章上架或下架", notes = "enable 上架: 1 下架: 0")
    @PostMapping("/down_or_up")
    public ResponseResult downOrUp(@RequestBody WmNewsDto dto) {
        return wmNewsService.downOrUp(dto);
    }

    /**
     * 修改文章
     *
     * @param wmNews
     * @return
     */
    @ApiOperation("根据id修改自媒体文章")
    @PutMapping("/update")
    public ResponseResult updateWmNews(@RequestBody WmNews wmNews) {
        wmNewsService.updateById(wmNews);
        return ResponseResult.okResult();
    }

    /**
     * 查询需要发布的文章id列表
     *
     * @return
     */
    @ApiOperation("查询待发布文章id列表")
    @GetMapping("/findRelease")
    public ResponseResult<List<Integer>> findRelease() {
        return ResponseResult.okResult(wmNewsService.findRelease());
    }

    /**
     * 查询文章列表
     * @param dto
     * @return
     */
    @PostMapping("/list_vo")
    public ResponseResult findList(@RequestBody NewsAuthDto dto) {
            return wmNewsService.findList(dto);
    }

    /**
     * 查询文章详情
     * @param id
     * @return
     */
    @GetMapping("/one_vo/{id}")
    public ResponseResult findWmNewsVo(@PathVariable("id") Integer id) {
        return wmNewsService.findWmNewsVo(id);
    }

    /**
     * 文章审核成功
     * @param dto
     * @return
     */
    @ApiOperation("人工审核通过 状态:4")
    @PostMapping("/auth_pass")
    public ResponseResult authPass(@RequestBody NewsAuthDto dto) {
        return wmNewsService.updateStatus(WemediaConstants.WM_NEWS_AUTH_PASS,dto);
    }

    /**
     * 文章审核失败
     * @param dto
     * @return
     */
    @ApiOperation("人工审核失败 状态:2")
    @PostMapping("/auth_fail")
    public ResponseResult authFail(@RequestBody NewsAuthDto dto) {
        return wmNewsService.updateStatus(WemediaConstants.WM_NEWS_AUTH_FAIL,dto);
    }
}
