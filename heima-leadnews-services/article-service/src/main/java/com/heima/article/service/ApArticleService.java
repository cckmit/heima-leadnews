package com.heima.article.service;
import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.model.article.dtos.ArticleDto;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.common.dtos.ResponseResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

public interface ApArticleService extends IService<ApArticle> {
    /**
     * 保存或修改文章
     * @param articleDto
     * @return
     */
    public ResponseResult saveArticle(ArticleDto articleDto);
}
