package com.heima.common.exception;

import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


@RestControllerAdvice
@Configuration
@Slf4j
public class ExceptionCatch {

    @ExceptionHandler(Exception.class)
    public ResponseResult exception(Exception ex){
        ex.printStackTrace();
        //记录日志
        log.error("Exception ex:{}",ex);
        return ResponseResult.errorResult(AppHttpCodeEnum.SERVER_ERROR,"您的网络异常，请稍后重试");
    }
    /**
     * 拦截自定义异常
     * @return
     */
    @ExceptionHandler(CustomException.class)
    public ResponseResult custException(CustomException ex) {
        ex.printStackTrace();
        log.error("CustomException ex:{}", ex);
        AppHttpCodeEnum codeEnum = ex.getAppHttpCodeEnum();
        return ResponseResult.errorResult(codeEnum);
    }
}
