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
    GeneratePageService generatePageService;

    @Autowired
    ApArticleMapper apArticleMapper;

    @Value("${file.minio.realPath}")
    String realPath;

    @Value("${file.oss.web-site}")
    String website;

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

    @Override
    public ResponseResult load(Short loadtype, ArticleHomeDto dto) {
        //1.校验参数  分页   频道标签  最大时间  最小时间   type类型 (0   1)
        if (dto.getSize()==null || dto.getSize()<1) {
            dto.setSize(10);
        }
        if (StringUtils.isBlank(dto.getTag())) {
            dto.setTag(ArticleConstants.DEFAULT_TAG);
        }
        if (dto.getMaxBehotTime()==null) {
            dto.setMaxBehotTime(new Date());
        }
        if (dto.getMinBehotTime()==null) {
            dto.setMinBehotTime(new Date());
        }
        if (!(loadtype.equals(ArticleConstants.LOADTYPE_LOAD_NEW)&&loadtype.equals(ArticleConstants.LOADTYPE_LOAD_MORE))) {
            loadtype=ArticleConstants.LOADTYPE_LOAD_MORE;
        }
        //2.业务实现，调用mapper进行两表查询
        List<ApArticle> apArticles = apArticleMapper.loadArticleList(dto, loadtype);
        for (ApArticle apArticle : apArticles) {
            apArticle.setStaticUrl(realPath+apArticle.getStaticUrl());
        }
        //3.返回结果
        ResponseResult responseResult = ResponseResult.okResult(apArticles);
        responseResult.setHost(website);
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
