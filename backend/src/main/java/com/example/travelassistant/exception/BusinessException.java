package com.example.travelassistant.exception;

/**
 * 自定义业务异常。
 * 用于表达“程序能继续运行，但当前请求不满足业务规则”的情况，
 * 例如用户名重复、景点不存在、无权限操作等。
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
