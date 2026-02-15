package com.kaos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Aplicación principal KAOS — Plataforma de Gestión de Equipos de Desarrollo.
 */
@SpringBootApplication
@EnableJpaAuditing
public class KaosApplication {

    public static void main(String[] args) {
        SpringApplication.run(KaosApplication.class, args);
    }
}
