package com.aiqutepets.config;

import com.aiqutepets.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import javax.servlet.http.HttpServletRequest;

/**
 * 全局异常处理器
 * 统一处理Controller层抛出的异常，返回标准格式的错误响应
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理参数缺失异常
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMissingParams(MissingServletRequestParameterException e, HttpServletRequest request) {
        log.warn("请求参数缺失: uri={}, param={}", request.getRequestURI(), e.getParameterName());
        return Result.error(400, "缺少必要参数: " + e.getParameterName());
    }

    /**
     * 处理参数类型不匹配异常
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleTypeMismatch(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        log.warn("参数类型不匹配: uri={}, param={}, value={}", 
                request.getRequestURI(), e.getName(), e.getValue());
        return Result.error(400, "参数类型错误: " + e.getName());
    }

    /**
     * 处理业务逻辑异常（RuntimeException）
     */
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleRuntimeException(RuntimeException e, HttpServletRequest request) {
        log.error("业务异常: uri={}, message={}", request.getRequestURI(), e.getMessage(), e);
        // 对外隐藏详细异常信息，只返回通用错误
        String message = e.getMessage();
        if (message != null && message.length() > 100) {
            message = message.substring(0, 100) + "...";
        }
        return Result.error(500, message != null ? message : "服务器内部错误");
    }

    /**
     * 处理第三方接口调用超时异常
     */
    @ExceptionHandler(ResourceAccessException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public Result<Void> handleResourceAccessException(ResourceAccessException e, HttpServletRequest request) {
        log.error("第三方服务调用超时: uri={}", request.getRequestURI(), e);
        return Result.error(503, "第三方服务暂时不可用，请稍后重试");
    }

    /**
     * 处理HTTP客户端错误异常
     */
    @ExceptionHandler(HttpClientErrorException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public Result<Void> handleHttpClientError(HttpClientErrorException e, HttpServletRequest request) {
        log.error("第三方接口调用失败: uri={}, status={}", 
                request.getRequestURI(), e.getStatusCode(), e);
        return Result.error(502, "第三方接口调用失败");
    }

    /**
     * 处理所有未捕获的异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e, HttpServletRequest request) {
        log.error("系统异常: uri={}", request.getRequestURI(), e);
        return Result.error(500, "服务器内部错误，请稍后重试");
    }

    /**
     * 处理非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleIllegalArgument(IllegalArgumentException e, HttpServletRequest request) {
        log.warn("非法参数: uri={}, message={}", request.getRequestURI(), e.getMessage());
        return Result.error(400, e.getMessage());
    }
}
