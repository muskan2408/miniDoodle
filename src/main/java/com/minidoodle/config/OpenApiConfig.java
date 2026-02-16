package com.minidoodle.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI miniDoodleOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Mini Doodle API")
                .description("A high-performance meeting scheduling platform REST API")
                .version("1.0.0")
                .contact(new Contact()
                    .name("Mini Doodle Team")
                    .email("support@minidoodle.com"))
                .license(new License()
                    .name("Apache 2.0")
                    .url("https://www.apache.org/licenses/LICENSE-2.0.html")));
    }
}
