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
import java.util.Arrays;
import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${website.preview-output-path:./preview-websites}")
    private String previewOutputPath;

    @Value("${website.templates-path:src/main/resources/templates}")
    private String templatesPath;

    // CORS配置 - 从配置文件读取允许的来源
    @Value("${cors.allowed-origins:http://localhost:5173,http://127.0.0.1:5173}")
    private String allowedOrigins;

    @Value("${cors.allow-credentials:true}")
    private boolean allowCredentials;

    @Value("${cors.max-age:3600}")
    private long maxAge;

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

        // 从配置文件解析允许的来源列表
        List<String> origins = Arrays.asList(allowedOrigins.split(","));
        for (String origin : origins) {
            String trimmedOrigin = origin.trim();
            if (!trimmedOrigin.isEmpty()) {
                config.addAllowedOrigin(trimmedOrigin);
            }
        }

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
        config.setAllowCredentials(allowCredentials);

        // 预检请求缓存时间（秒）
        config.setMaxAge(maxAge);

        // 注册 CORS 配置，应用于所有路径
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
