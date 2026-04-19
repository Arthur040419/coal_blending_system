package com.coalblend.common.exception;

import com.coalblend.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusiness(BusinessException e) {
        return Result.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public Result<Void> handleValidation(Exception e) {
        String msg = e.getMessage();
        if (e instanceof MethodArgumentNotValidException m) {
            var field = m.getBindingResult().getFieldError();
            if (field != null) {
                msg = field.getDefaultMessage();
            }
        } else if (e instanceof BindException b) {
            var field = b.getBindingResult().getFieldError();
            if (field != null) {
                msg = field.getDefaultMessage();
            }
        }
        return Result.fail(400, msg != null ? msg : "参数校验失败");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleNotReadable(HttpMessageNotReadableException e) {
        return Result.fail(400, "请求体格式错误");
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleOther(Exception e) {
        log.error("Unhandled error", e);
        return Result.fail(500, "系统繁忙，请稍后重试");
    }
}
