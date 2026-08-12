package com.sky.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 健康检查控制器
 * <p>
 * 提供一个简单的 GET 接口，用于验证后端服务是否正常运行
 */
@RestController
public class HelloController {

    /**
     * 健康检查接口
     *
     * @return 固定问候字符串
     */
    @GetMapping("/hello")
    public String hello() {
        return "Hello, Sky Takeout!";
    }
}
