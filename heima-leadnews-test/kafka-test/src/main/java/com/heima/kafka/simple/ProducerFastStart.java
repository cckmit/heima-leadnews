package com.heima.kafka.simple;

import org.apache.kafka.clients.producer.*;

import java.util.Properties;
import java.util.concurrent.ExecutionException;

/**
 * @作者 itcast
 * @创建日期 2021/8/9 9:32
 **/
public class ProducerFastStart {
    private static final String TOPIC = "itcast-heima";
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        // 封装kafka配置信息
        Properties properties = new Properties();
        // kafka 服务端地址
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,"192.168.200.130:9092");
        // key 和 value的序列化方式
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,"org.apache.kafka.common.serialization.StringSerializer");
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,"org.apache.kafka.common.serialization.StringSerializer");
        properties.put(ProducerConfig.RETRIES_CONFIG,10); // 重试次数
        properties.put(ProducerConfig.ACKS_CONFIG,"0"); // 重试次数
        KafkaProducer<String,String>  producer = new KafkaProducer<String, String>(properties);

        ProducerRecord<String, String> record = new ProducerRecord<>(TOPIC, "key001","hello kafka 传智播客");

//        RecordMetadata recordMetadata = producer.send(record).get();// 发送消息
//        System.out.println("消息发送到的分区==> " + recordMetadata.partition() + "  偏移量==>"+recordMetadata.offset());

        producer.send(record, new Callback() {
            @Override
            public void onCompletion(RecordMetadata recordMetadata, Exception e) {
                System.out.println("异步消息结果  消息发送到的分区==> " + recordMetadata.partition() + "  偏移量==>"+recordMetadata.offset());
            }
        });
        System.out.println("   消息发送完毕后的结果打印 ");
        producer.close();
    }
}
