package com.holdhive.common.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI holdHiveOpenApi() {
        return new OpenAPI()
            .info(new Info()
                .title("HoldHive API")
                .version("0.1.0")
                .description("Portfolio holdings, valuation, pricing, and health APIs."))
            .servers(List.of(new Server()
                .url("/")
                .description("Current host")));
    }
}
