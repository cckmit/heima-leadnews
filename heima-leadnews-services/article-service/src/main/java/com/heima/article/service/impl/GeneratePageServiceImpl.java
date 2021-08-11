package com.heima.article.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.heima.article.mapper.ApArticleConfigMapper;
import com.heima.article.mapper.ApArticleContentMapper;
import com.heima.article.mapper.ApArticleMapper;
import com.heima.article.mapper.AuthorMapper;
import com.heima.article.service.GeneratePageService;
import com.heima.common.exception.CustException;
import com.heima.file.service.FileStorageService;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.article.pojos.ApArticleContent;
import com.heima.model.article.pojos.ApAuthor;
import com.heima.model.common.enums.AppHttpCodeEnum;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @作者 itcast
 * @创建日期 2021/8/10 15:28
 **/
@Service
@Slf4j
public class GeneratePageServiceImpl implements GeneratePageService {
    @Autowired
    ApArticleMapper apArticleMapper;

    @Autowired
    ApArticleContentMapper apArticleContentMapper;

    @Autowired
    AuthorMapper authorMapper;

    @Autowired
    Configuration configuration;

    @Resource(name = "minIOFileStorageService")
    FileStorageService fileStorageService;


    @Async
    @Override
    public void generateArticlePage(Long articleId) {
        log.info(" 静态页生成方法被触发 =================   articleId ==> {}",articleId);
        // 1. 根据文章id查询文章信息  文章内容   文章作者信息
        ApArticle apArticle = apArticleMapper.selectById(articleId);
        if(apArticle == null){
            CustException.cust(AppHttpCodeEnum.DATA_NOT_EXIST);
        }
        ApArticleContent apArticleContent = apArticleContentMapper.selectOne(Wrappers.<ApArticleContent>lambdaQuery().eq(ApArticleContent::getArticleId, articleId));
        if(apArticleContent == null){
            CustException.cust(AppHttpCodeEnum.DATA_NOT_EXIST);
        }
        ApAuthor author = authorMapper.selectById(apArticle.getAuthorId());
        if(author == null){
            CustException.cust(AppHttpCodeEnum.DATA_NOT_EXIST);
        }
        List<Map> content = JSONArray.parseArray(apArticleContent.getContent(), Map.class);
        // 2. 准备数据模型
        Map params = new HashMap<>();
        params.put("authorApUserId",author.getUserId());
        params.put("article",apArticle);
        params.put("content",content);
        // 3. 获取模板
        try {
            Template template = configuration.getTemplate("article.ftl");
            // 4. 调用模板引擎API 替换模板内容
            StringWriter stringWriter = new StringWriter();
            template.process(params,stringWriter);
            InputStream inputStream = new ByteArrayInputStream(stringWriter.toString().getBytes());
            // minIO   inputStream
            String htmlPath = fileStorageService.store("article", apArticle.getId() + ".html", "text/html", inputStream);
            apArticle.setStaticUrl(htmlPath);
            apArticleMapper.updateById(apArticle);
            log.info("页面静态化 成功     页面路径: {}",htmlPath);
        } catch (Exception e) {
            e.printStackTrace();
            log.error(" 页面静态化生成失败 , 原因: {}",e.getMessage());
            CustException.cust(AppHttpCodeEnum.SERVER_ERROR,"页面静态化生成失败"+e.getMessage());
        }
    }
}
