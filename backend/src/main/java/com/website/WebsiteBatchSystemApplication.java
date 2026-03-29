package com.website;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class WebsiteBatchSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebsiteBatchSystemApplication.class, args);
        System.out.println("========================================");
        System.out.println("网站批量生成系统启动成功！");
        System.out.println("访问地址: http://localhost:8080/api");
        System.out.println("========================================");
    }
}
