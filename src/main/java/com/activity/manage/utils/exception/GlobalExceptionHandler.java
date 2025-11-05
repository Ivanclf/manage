package com.activity.manage.utils.exception;

import com.activity.manage.utils.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 处理BaseException及其子类
     * @param ex
     * @return
     */
    @ExceptionHandler(BaseException.class)
    public Result exceptionHandler(BaseException ex) {
        log.error("业务异常：{}", ex.getMessage());
        return Result.error(ex.getMessage());
    }
    
    /**
     * 处理非法参数异常
     * @param ex
     * @return
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Result exceptionHandler(IllegalArgumentException ex) {
        log.error("参数异常：{}", ex.getMessage());
        return Result.error("参数错误：" + ex.getMessage());
    }
    
    /**
     * 处理其他未捕获的异常
     * @param ex
     * @return
     */
    @ExceptionHandler(Exception.class)
    public Result exceptionHandler(Exception ex) {
        log.error("系统异常：{}", ex.getMessage(), ex);
        return Result.error("系统错误，请联系管理员");
    }
}
