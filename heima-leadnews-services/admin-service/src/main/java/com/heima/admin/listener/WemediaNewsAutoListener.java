package com.heima.admin.listen;

import com.alibaba.fastjson.JSON;
import com.heima.admin.service.WemediaNewsAutoScanService;
import com.heima.common.constants.message.NewsAutoScanConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @作者 itcast
 * @创建日期 2021/8/10 14:44
 **/

@Component
@Slf4j
public class WemediaNewsAutoListener {
    @Autowired
    WemediaNewsAutoScanService wemediaNewsAutoScanService;
    @KafkaListener(topics = NewsAutoScanConstants.WM_NEWS_AUTO_SCAN_TOPIC,errorHandler = "kafkaExceptionHandler")
    public void handleAutoScanMsg(String message){
        log.info("接收到文章自动审核消息   消息内容为===> {}",message);
        Map map = JSON.parseObject(message, Map.class);
        Object newsId = map.get("newsId");
        wemediaNewsAutoScanService.autoScanByMediaNewsId((Integer)newsId);
        log.info("文章自动审核消息处理完毕 =============");
    }
}
