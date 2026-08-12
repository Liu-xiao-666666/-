package com.sky.handler;

import com.sky.vo.Result;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 * <p>
 * 拦截 Controller 层抛出的 RuntimeException，
 * 统一转换为 Result 格式的错误响应返回给前端
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理运行时异常，将异常信息包装为统一错误响应
     *
     * @param e 运行时异常
     * @return 包含错误信息的统一响应体
     */
    @ExceptionHandler(RuntimeException.class)
    public Result<Void> handleRuntimeException(RuntimeException e) {
        return Result.error(e.getMessage());
    }
}
