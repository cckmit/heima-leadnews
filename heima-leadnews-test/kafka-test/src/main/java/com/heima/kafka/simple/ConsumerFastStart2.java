package com.heima.kafka.simple;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;

/**
 * @作者 itcast
 * @创建日期 2021/8/9 9:39
 **/
public class ConsumerFastStart2 {
    private static final String TOPIC = "itcast-heima";
    public static void main(String[] args) {

        // 消费者配置
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,"192.168.200.130:9092");
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,"org.apache.kafka.common.serialization.StringDeserializer");
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,"org.apache.kafka.common.serialization.StringDeserializer");
        //设置分组
        properties.put(ConsumerConfig.GROUP_ID_CONFIG,"group1");
        KafkaConsumer<String,String> kafkaConsumer = new KafkaConsumer<String, String>(properties);

        kafkaConsumer.subscribe(Arrays.asList(TOPIC));

        while (true){
            ConsumerRecords<String, String> records = kafkaConsumer.poll(Duration.ofMillis(1000));
            for (ConsumerRecord<String, String> record : records) {
                System.out.println("得到主题==>" + record.topic() + "   消息的key==>" + record.key()   + "   消息的value==>"+record.value());
            }
        }


    }
}
