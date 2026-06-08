package com.fallguys.itemservice.controller;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class OpenApiConfig {

    @Bean
    OpenAPI itemServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Item Service API")
                        .version("v1")
                        .description("ERP Item 서비스의 부품 마스터 공개 API 문서입니다."));
    }
}
