package com.website.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${website.preview-output-path:./preview-websites}")
    private String previewOutputPath;

    @Value("${website.templates-path:src/main/resources/templates}")
    private String templatesPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 映射预览网站目录到 /api/preview/** URL 路径
        String absolutePreviewPath = Paths.get(previewOutputPath).toAbsolutePath().toString();
        if (!absolutePreviewPath.endsWith("/")) {
            absolutePreviewPath += "/";
        }

        // 主预览路径 - 处理所有预览网站文件（包括 index.html 和静态资源）
        // 请求 /api/preview/{domain}-preview/static/css/style.css
        // 映射到 {previewOutputPath}/{domain}-preview/static/css/style.css
        registry.addResourceHandler("/api/preview/**")
                .addResourceLocations("file:" + absolutePreviewPath);

        // 备用预览路径
        registry.addResourceHandler("/preview/**")
                .addResourceLocations("file:" + absolutePreviewPath);

        // 映射模板静态资源到 /static/** URL 路径
        String absoluteTemplatePath = Paths.get(templatesPath).toAbsolutePath().toString();
        if (!absoluteTemplatePath.endsWith("/")) {
            absoluteTemplatePath += "/";
        }
        registry.addResourceHandler("/static/**")
                .addResourceLocations("file:" + absoluteTemplatePath + "static/");
    }

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();

        // 允许的来源
        config.addAllowedOrigin("http://localhost:5173");
        config.addAllowedOrigin("http://127.0.0.1:5173");
        // 服务器部署地址
        config.addAllowedOrigin("http://124.223.45.101");
        config.addAllowedOrigin("http://124.223.45.101:8088");
        config.addAllowedOrigin("http://124.223.45.101:80");
        // 允许所有来源用于测试（生产环境应限制）
        config.addAllowedOriginPattern("*");

        // 允许的 HTTP 方法
        config.addAllowedMethod("OPTIONS");
        config.addAllowedMethod("GET");
        config.addAllowedMethod("POST");
        config.addAllowedMethod("PUT");
        config.addAllowedMethod("DELETE");
        config.addAllowedMethod("PATCH");

        // 允许的请求头
        config.addAllowedHeader("Origin");
        config.addAllowedHeader("Content-Type");
        config.addAllowedHeader("Accept");
        config.addAllowedHeader("Authorization");
        config.addAllowedHeader("X-Requested-With");

        // 允许暴露的响应头
        config.addExposedHeader("Authorization");
        config.addExposedHeader("Content-Disposition");

        // 允许携带凭证
        config.setAllowCredentials(true);

        // 预检请求缓存时间（秒）
        config.setMaxAge(3600L);

        // 注册 CORS 配置，应用于所有路径
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
