package com.heima.kafka.listen;

import com.alibaba.fastjson.JSON;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @作者 itcast
 * @创建日期 2021/8/9 11:29
 **/
@Component
public class MessageListener {
//    @KafkaListener(topics = "itcast-topic")
//    public void handleMsg(ConsumerRecord<String,String> record){
//        System.out.println("消息key=>"+record.key() + "  消息value=>"+record.value());
//    }

    @KafkaListener(topics = "itcast-topic")
    public void handleMsg(String msg){ //"{}"
        System.out.println("  消息value=>"+msg);
        Map map = JSON.parseObject(msg, Map.class);
        System.out.println("name==>"+map.get("name"));
    }
}
