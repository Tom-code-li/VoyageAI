package com.example.travelassistant.exception;

import com.example.travelassistant.common.Result;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器。
 * 统一把后端异常转换成前端可消费的 Result 结构，
 * 避免前端拿到原始 HTML 错误页或难以解析的异常对象。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常：通常是用户输入不满足业务规则。 */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusiness(BusinessException exception) {
        return Result.fail(400, exception.getMessage());
    }

    /** 参数校验异常：请求体缺字段、类型不匹配、校验注解不通过等。 */
    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            BindException.class,
            ConstraintViolationException.class,
            HttpMessageNotReadableException.class
    })
    public Result<Void> handleValidation(Exception exception) {
        return Result.fail(400, "请求参数不合法: " + exception.getMessage());
    }

    /** 兜底异常：避免未处理异常直接把堆栈暴露给前端。 */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception exception) {
        return Result.fail(500, "服务器内部异常: " + exception.getMessage());
    }
}
