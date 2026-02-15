package com.kaos.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.context.annotation.Bean;

import java.util.Optional;

/**
 * Configuración de test para simular JPA Auditing sin requerir DataSource.
 * Usado en tests @WebMvcTest para evitar errores de "JPA metamodel must not be empty".
 */
@TestConfiguration
public class TestJpaAuditConfig {

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> Optional.of("test-user");
    }
}
