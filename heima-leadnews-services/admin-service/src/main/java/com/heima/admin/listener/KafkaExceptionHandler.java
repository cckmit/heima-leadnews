package com.heima.admin.listener;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.springframework.kafka.listener.KafkaListenerErrorHandler;
import org.springframework.kafka.listener.ListenerExecutionFailedException;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

@Component("kafkaExceptionHandler")
@Slf4j
public class KafkaExceptionHandler implements KafkaListenerErrorHandler {
    @Override
    public Object handleError(Message<?> message, ListenerExecutionFailedException e) {
        log.error("kafka处理消息出现异常  消息内容==> {}   异常信息==> {} ",message.getPayload().toString(),e.getMessage());
        return null;
    }
    @Override
    public Object handleError(Message<?> message, ListenerExecutionFailedException exception, Consumer<?, ?> consumer) {
        log.error("kafka处理消息出现异常  消息内容==> {}   异常信息==> {} ",message.getPayload().toString(),exception.getMessage());
        return null;
    }
}
