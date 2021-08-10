package com.heima.kafka.controller;

import com.alibaba.fastjson.JSON;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * @作者 itcast
 * @创建日期 2021/8/9 11:26
 **/
@RestController
public class HelloController {
    @Autowired
    KafkaTemplate<String,String> kafkaTemplate;
    @RequestMapping("hello") //  hello?msg=213213213
    public String hello(String msg){
        Map map = new HashMap<>();
        map.put("name","xiaoming");
        map.put("age","28");
        map.put("phone","13812345678");
        kafkaTemplate.send("itcast-topic","spring-kafka", JSON.toJSONString(map));
        return "ok";
    }
}
