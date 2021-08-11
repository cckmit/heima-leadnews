package com.heima.article.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.article.mapper.ApArticleConfigMapper;
import com.heima.article.mapper.ApArticleContentMapper;
import com.heima.article.mapper.ApArticleMapper;
import com.heima.article.mapper.AuthorMapper;
import com.heima.article.service.ApArticleService;
import com.heima.article.service.GeneratePageService;
import com.heima.common.exception.CustException;
import com.heima.model.article.dtos.ArticleDto;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.article.pojos.ApArticleConfig;
import com.heima.model.article.pojos.ApArticleContent;
import com.heima.model.article.pojos.ApAuthor;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ApArticleServiceImpl extends ServiceImpl<ApArticleMapper, ApArticle> implements ApArticleService {

    @Autowired
    GeneratePageService generatePageService;

    @Override
    public ResponseResult saveArticle(ArticleDto articleDto) {
        // 保存文章
        // 基于articleDao创建apArticle
        ApArticle apArticle = getApArticle(articleDto);
        // 保存或修改文章
        saveOrUpdateArticle(apArticle);
        // 通过apArticle和articleDto来保存 apArticleConfig apArticleContent
        saveConfigAndContent(articleDto, apArticle);
        generatePageService.generateArticlePage(apArticle.getId());
        return ResponseResult.okResult(apArticle.getId());
    }

    private void saveConfigAndContent(ArticleDto articleDto, ApArticle apArticle) {
        ApArticleConfig apArticleConfig = new ApArticleConfig();
        apArticleConfig.setArticleId(apArticle.getId());
        apArticleConfig.setIsComment(true);
        apArticleConfig.setIsForward(true);
        apArticleConfig.setIsDown(false);
        apArticleConfig.setIsDelete(false);
        apArticleConfigMapper.insert(apArticleConfig);

        ApArticleContent apArticleContent = new ApArticleContent();
        apArticleContent.setArticleId(apArticle.getId());
        apArticleContent.setContent(articleDto.getContent());
        apArticleContentMapper.insert(apArticleContent);
    }

    @Autowired
    ApArticleConfigMapper apArticleConfigMapper;

    @Autowired
    ApArticleContentMapper apArticleContentMapper;

    private void saveOrUpdateArticle(ApArticle apArticle) {
        if (apArticle.getId() == null) {
            // 保存文章
            apArticle.setCollection(0); // 收藏数
            apArticle.setLikes(0);// 点赞数
            apArticle.setComment(0);// 评论数
            apArticle.setViews(0); // 阅读数
            save(apArticle);
        } else {
            // 修改文章  删除之前关联的配置信息   内容信息
            ApArticle oldArticle = getById(apArticle.getId());
            if (oldArticle == null) {
                CustException.cust(AppHttpCodeEnum.DATA_NOT_EXIST);
            }
            updateById(apArticle);
            apArticleConfigMapper.delete(Wrappers.<ApArticleConfig>lambdaQuery().eq(ApArticleConfig::getArticleId, oldArticle.getId()));
            apArticleContentMapper.delete(Wrappers.<ApArticleContent>lambdaQuery().eq(ApArticleContent::getArticleId, oldArticle.getId()));
        }
    }

    @Autowired
    AuthorMapper authorMapper;

    private ApArticle getApArticle(ArticleDto articleDto) {
        ApArticle apArticle = new ApArticle();
        BeanUtils.copyProperties(articleDto, apArticle);
        Integer wmUserId = articleDto.getWmUserId();
        ApAuthor apAuthor = authorMapper.selectOne(Wrappers.<ApAuthor>lambdaQuery().eq(ApAuthor::getWmUserId, wmUserId));
        if (apAuthor != null) {
            apArticle.setAuthorName(apAuthor.getName());
            apArticle.setAuthorId(Long.valueOf(apAuthor.getId()));
        }
        // apArticle中apAuthor的id&name apAuthor可以通过wmUser查到 wmUser可以通过wmNews查到

        return apArticle;
    }

}
