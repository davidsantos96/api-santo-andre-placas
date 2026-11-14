package br.com.santoandreplacas.apisantoandreplacas.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/** Em producao a API recusa subir com os segredos em branco ou fracos. */
@Component
@Profile("prod")
public class ProducaoSegura {

    @Value("${app.jwt.secret}") private String jwtSecret;
    @Value("${app.cors.allowed-origins}") private String origens;

    @PostConstruct
    void validar() {
        if (jwtSecret.isBlank() || jwtSecret.length() < 32) {
            throw new IllegalStateException("Producao exige JWT_SECRET proprio (minimo 32 caracteres).");
                }
        if (origens.contains("localhost") || origens.contains("127.0.0.1")) {
            throw new IllegalStateException("Producao exige ALLOWED_ORIGINS com a URL real do front.");
        }
    }
}
