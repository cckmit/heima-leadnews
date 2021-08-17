package com.heima.article.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.article.mapper.ApArticleConfigMapper;
import com.heima.article.mapper.ApArticleContentMapper;
import com.heima.article.mapper.ApArticleMapper;
import com.heima.article.mapper.AuthorMapper;
import com.heima.article.service.ApArticleService;
import com.heima.article.service.GeneratePageService;
import com.heima.common.constants.article.ArticleConstants;
import com.heima.common.exception.CustException;
import com.heima.model.article.dtos.ArticleDto;
import com.heima.model.article.dtos.ArticleHomeDto;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.article.pojos.ApArticleConfig;
import com.heima.model.article.pojos.ApArticleContent;
import com.heima.model.article.pojos.ApAuthor;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;


@Service
@Slf4j
public class ApArticleServiceImpl extends ServiceImpl<ApArticleMapper, ApArticle> implements ApArticleService {

    @Autowired
    AuthorMapper authorMapper;

    @Autowired
    ApArticleConfigMapper apArticleConfigMapper;
    @Autowired
    ApArticleContentMapper apArticleContentMapper;

    @Autowired
    GeneratePageService generatePageService;

    @Autowired
    ApArticleMapper apArticleMapper;

    @Value("${file.minio.readPath}")
    String readPath;

    @Value("${file.oss.web-site}")
    String webSite;


    @Override
    public ResponseResult saveArticle(ArticleDto articleDto) {
        //1. 基于articleDto  创建ApArticle对象
        ApArticle apArticle = getApArticle(articleDto);
        //2. 保存或修改apArticle
        saveOrUpdateArticle(apArticle);
        //3. 保存 config信息 及 content信息
        saveConfigAndContent(articleDto, apArticle);
        //4. 页面静态化
        generatePageService.generateArticlePage(apArticle.getId());
        log.info("静态生成方法已经调用");
        return  ResponseResult.okResult(apArticle.getId());// 文章的id
    }

    @Override
    public ResponseResult load(Short loadtype, ArticleHomeDto dto) {
        // 1. 检查参数    分页   频道标签  最大时间  最小时间   type类型 (0   1)
        if(dto.getSize() == null || dto.getSize() < 1){
            dto.setSize(10);
        }
        if (StringUtils.isBlank(dto.getTag())) {
            dto.setTag(ArticleConstants.DEFAULT_TAG);
        }
        if(dto.getMaxBehotTime() == null){
            dto.setMaxBehotTime(new Date());
        }
        if(dto.getMinBehotTime() == null){
            dto.setMinBehotTime(new Date());
        }
        if(!loadtype.equals(ArticleConstants.LOADTYPE_LOAD_MORE) && !loadtype.equals(ArticleConstants.LOADTYPE_LOAD_NEW)){
            loadtype = ArticleConstants.LOADTYPE_LOAD_MORE;
        }
        // 2. 调用mapper进行两表查询
        List<ApArticle> apArticles = apArticleMapper.loadArticleList(dto, loadtype);
        for (ApArticle apArticle : apArticles) {
            apArticle.setStaticUrl(readPath + apArticle.getStaticUrl());
        }
        // 3. 封装返回结果   (host==>图片前缀    static_url => 拼接 前缀路径)
        ResponseResult responseResult = ResponseResult.okResult(apArticles);
        responseResult.setHost(webSite);
        return responseResult;
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

    private void saveOrUpdateArticle(ApArticle apArticle) {
        if(apArticle.getId() == null){
            // 保存文章
            apArticle.setCollection(0); // 收藏数
            apArticle.setLikes(0);// 点赞数
            apArticle.setComment(0);// 评论数
            apArticle.setViews(0); // 阅读数
            save(apArticle);
        }else {
            // 修改文章  删除之前关联的配置信息   内容信息
            ApArticle article = getById(apArticle.getId());
            if(article == null){
                CustException.cust(AppHttpCodeEnum.DATA_NOT_EXIST);
            }
            updateById(apArticle);
            apArticleConfigMapper.delete(Wrappers.<ApArticleConfig>lambdaQuery().eq(ApArticleConfig::getArticleId,apArticle.getId()));
            apArticleContentMapper.delete(Wrappers.<ApArticleContent>lambdaQuery().eq(ApArticleContent::getArticleId,apArticle.getId()));
        }
    }

    private ApArticle getApArticle(ArticleDto articleDto) {
        ApArticle apArticle = new ApArticle();
        BeanUtils.copyProperties(articleDto,apArticle);
        // 基于wmUserId查询作者信息
        ApAuthor author = authorMapper.selectOne(Wrappers.<ApAuthor>lambdaQuery().eq(ApAuthor::getWmUserId, articleDto.getWmUserId()));
        if(author!=null){
            apArticle.setAuthorId(Long.valueOf(author.getId()));
            apArticle.setAuthorName(author.getName());
        }
        return apArticle;
    }
}
