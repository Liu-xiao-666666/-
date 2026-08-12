package com.sky.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置类
 * <p>
 * 将本地文件目录映射为 HTTP 静态资源路径，
 * 使上传的图片可以通过 /uploads/** 直接访问
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${upload.path:D:/temp}")
    private String uploadPath;

    /**
     * 配置静态资源映射，将 uploadPath 目录挂载到 /uploads/** 路径
     *
     * @param registry 资源处理器注册表
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadPath + "/");
    }
}
