package br.edu.unifsa.sinasc.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Documentação Swagger/OpenAPI. Interface em /swagger-ui.html e JSON em /v3/api-docs.
 * Para testar rotas protegidas: faça login, copie o token e use o botão "Authorize".
 */
@Configuration
@OpenAPIDefinition(info = @Info(
        title = "API SINASC - Risco de baixo peso ao nascer",
        version = "0.1.0",
        description = "Sistema de apoio à decisão para UBS. Back-end em Spring Boot."))
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT")
public class OpenApiConfig {
}
