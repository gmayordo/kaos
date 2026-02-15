package com.kaos.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;

/**
 * Configuración de OpenAPI / Swagger UI.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI kaosOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("KAOS API")
                        .description("API de la Plataforma de Gestión de Equipos de Desarrollo")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Equipo KAOS")
                                .email("kaos@ehcos.com")));
    }
}
