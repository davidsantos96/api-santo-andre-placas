package br.com.santoandreplacas.apisantoandreplacas.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/** Em producao a API recusa subir com os valores padrao que estao no repositorio publico. */
@Component
@Profile("prod")
public class ProducaoSegura {

    private static final String JWT_PADRAO = "chave-secreta-temporaria-para-estudo-trocar-depois-1234567890";

    @Value("${app.jwt.secret}") private String jwtSecret;
    @Value("${app.seed.admin.senha}") private String senhaAdmin;
    @Value("${app.cors.allowed-origins}") private String origens;

    @PostConstruct
    void validar() {
        if (JWT_PADRAO.equals(jwtSecret) || jwtSecret.length() < 32) {
            throw new IllegalStateException("Producao exige JWT_SECRET proprio (minimo 32 caracteres).");
        }
        if ("admin123".equals(senhaAdmin) || senhaAdmin.length() < 10) {
            throw new IllegalStateException("Producao exige ADMIN_SEED_PASSWORD proprio (minimo 10 caracteres).");
        }
        if (origens.contains("localhost") || origens.contains("127.0.0.1")) {
            throw new IllegalStateException("Producao exige ALLOWED_ORIGINS com a URL real do front.");
        }
    }
}
