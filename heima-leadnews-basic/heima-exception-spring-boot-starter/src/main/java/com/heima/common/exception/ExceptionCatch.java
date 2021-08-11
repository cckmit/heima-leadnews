package com.heima.common.exception;

import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 异常处理类
 * @作者 itcast
 * @创建日期 2021/8/3 8:58
 **/

@RestControllerAdvice // controller增强注解
@Slf4j
public class ExceptionCatch {
    @ExceptionHandler(Exception.class)
    public ResponseResult exceptionHandler(Exception e){
        e.printStackTrace();
        log.error("统一捕获异常:   {}",e);
        return ResponseResult.errorResult(AppHttpCodeEnum.SERVER_ERROR,"服务器出现未知异常，请稍后重试");
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


    /**
     * 参数校验
     * @return
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseResult handleValidationException(MethodArgumentNotValidException ex) {
        log.error("MethodArgumentNotValidException ex:{}", ex);
        ex.printStackTrace();
        return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID, ex.getBindingResult().getFieldError().getDefaultMessage());
    }
}
