package com.heima.article.listener;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.heima.article.service.ApArticleConfigService;
import com.heima.common.constants.message.WmNewsMessageConstants;
import com.heima.model.article.pojos.ApArticleConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
public class ArticleIsDownListener {
    @Autowired
    private ApArticleConfigService apArticleConfigService;
    @KafkaListener(topics = WmNewsMessageConstants.WM_NEWS_UP_OR_DOWN_TOPIC)
    public void receiveMessage(String message) {
        log.info("收到 主题: {}  消息， 参数为: {}",WmNewsMessageConstants.WM_NEWS_UP_OR_DOWN_TOPIC,message);
        Map map = JSON.parseObject(message, Map.class);
        Object enable = map.get("enable");
        boolean isDown = enable.equals(1) ? false : true;
        apArticleConfigService.update(Wrappers.<ApArticleConfig>lambdaUpdate()
                .eq(ApArticleConfig::getArticleId, map.get("articleId")).set(ApArticleConfig::getIsDown, isDown));
    }
}
