package com.sky.config;

import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Tomcat 容器配置类
 * <p>
 * 自定义嵌入式 Tomcat 的连接器参数，
 * 增大 HTTP POST 请求体大小限制，以支持文件上传
 */
@Configuration
public class TomcatConfig {

    /**
     * 配置 Tomcat 连接器最大 POST 大小为 10MB
     *
     * @return WebServerFactoryCustomizer 自定义器
     */
    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatCustomizer() {
        return factory -> factory.addConnectorCustomizers(connector ->
                connector.setMaxPostSize(10 * 1024 * 1024));
    }
}
