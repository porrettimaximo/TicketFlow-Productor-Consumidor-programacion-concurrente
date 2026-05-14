package com.ticketflow.ventas.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * ============================================================
 * Configuración CORS
 * ============================================================
 * Permite que el frontend React (en puerto 5173) pueda
 * comunicarse con el backend Spring Boot (en puerto 8080).
 * Sin esta configuración, el navegador bloquearía las peticiones.
 */
@Configuration
public class WebConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // Permitir el origen del frontend de desarrollo
        config.addAllowedOriginPattern("*");

        // Permitir todos los métodos HTTP necesarios para la API REST
        config.addAllowedMethod("GET");
        config.addAllowedMethod("POST");
        config.addAllowedMethod("DELETE");
        config.addAllowedMethod("OPTIONS");

        // Permitir todos los headers
        config.addAllowedHeader("*");

        // Permitir credenciales (cookies, auth headers)
        config.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}
