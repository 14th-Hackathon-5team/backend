package com.example.kbuddy.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        Info info = new Info()
                .title("kbuddy")
                .description("한국에 온 외국인/유학생의 한국 정착과 생활 적응을 돕는 AI 기반 서비스")
                .version("0.0.1-SNAPSHOT");

        return new OpenAPI().info(info);
    }
}
