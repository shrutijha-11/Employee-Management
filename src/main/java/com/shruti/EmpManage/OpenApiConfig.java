package com.shruti.EmpManage.Config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI employeeApiInfo() {
        return new OpenAPI().info(new Info()
                .title("Employee Management API")
                .version("1.0")
                .description("REST API for managing employees, with pagination, search, "
                        + "salary filtering and department summaries."));
    }
}