package com.sky;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 天空外卖系统 - Spring Boot 启动类
 * <p>
 * 自动扫描 com.sky 包下所有组件，启动嵌入式 Tomcat 服务器，
 * 默认监听 8080 端口，上下文路径为 /api
 */
@SpringBootApplication
public class SkyTakeOutApplication {

    /**
     * 应用程序主入口
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(SkyTakeOutApplication.class, args);
    }
}
