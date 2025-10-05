package com.ll.P_A.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    // 전체 문서 메타 + JWT(Bearer) 인증 스키마 설정
    @Bean
    public OpenAPI openAPI() {
        String bearerSchemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("Project-A API")
                        .description("Project-A 백엔드 OpenAPI 문서")
                        .version("v1.0.0")
                        .contact(new Contact().name("Team11").email("contact@example.com"))
                )
                .addSecurityItem(new SecurityRequirement().addList(bearerSchemeName))
                .components(new Components()
                        .addSecuritySchemes(bearerSchemeName,
                                new SecurityScheme()
                                        .name(bearerSchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }

    // 문서에 포함할 패키지/경로 그룹 (원하는 대로 조정 가능)
    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("v1")
                .packagesToScan("com.ll.P_A") // 컨트롤러 패키지 스캔
                .pathsToMatch("/api/**")      // /api/** 만 문서화
                .build();
    }
}