package com.heima.article.listen;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.heima.article.mapper.ApArticleConfigMapper;
import com.heima.article.service.ApArticleConfigService;
import com.heima.common.constants.message.WmNewsMessageConstants;
import com.heima.model.article.pojos.ApArticleConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @作者 itcast
 * @创建日期 2021/8/12 15:33
 **/
@Component
@Slf4j
public class ArticleIsDownListener {
    @Autowired
    ApArticleConfigService apArticleConfigService;
    @KafkaListener(topics = WmNewsMessageConstants.WM_NEWS_UP_OR_DOWN_TOPIC)
    public void downOrUpHandler(String message){
        log.info("接收到文章上下架消息  ，消息内容==>{}",message);
        if (StringUtils.isNotBlank(message)) {
            Map map = JSON.parseObject(message, Map.class);
            Object articleId = map.get("articleId");
            Integer enable = (Integer)map.get("enable"); // 1 上架  0 下架
            // false 上架  true 下架
            boolean isDown = enable.intValue() == 1?false:true;
            apArticleConfigService.update(Wrappers.<ApArticleConfig>lambdaUpdate()
                    .set(ApArticleConfig::getIsDown,isDown)
                    .eq(ApArticleConfig::getArticleId,articleId)
            );
        }
        log.info("成功修改文章上下架状态 =========================");
    }

}
