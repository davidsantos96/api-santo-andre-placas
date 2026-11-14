package br.com.santoandreplacas.apisantoandreplacas.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey chave;

    public JwtService(@Value("${app.jwt.secret:}") String segredo) {
        if (segredo == null || segredo.isBlank()) {
            // Sem JWT_SECRET (só em desenvolvimento; em produção o ProducaoSegura recusa):
            // chave aleatória desta execução, os logins caem ao reiniciar.
            byte[] aleatoria = new byte[48];
            new java.security.SecureRandom().nextBytes(aleatoria);
            this.chave = Keys.hmacShaKeyFor(aleatoria);
        } else {
            this.chave = Keys.hmacShaKeyFor(segredo.getBytes(StandardCharsets.UTF_8));
        }
    }

    private final long expiracaoMs = 28_800_000; // 8 horas — cobre um turno de trabalho; sem refresh token (decisão registrada no CONTEXT.md)

    public String gerarToken(String login, String papel) {
        Date agora = new Date();
        Date expiracao = new Date(agora.getTime() + expiracaoMs);

        return Jwts.builder()
                .subject(login)
                .claim("papel", papel)
                .issuedAt(agora)
                .expiration(expiracao)
                .signWith(chave)
                .compact();
    }

    /** Login do usuário (tokens emitidos antes do campo login trazem o e-mail; ambos são aceitos na busca). */
    public String extrairLogin(String token) {
        return Jwts.parser()
                .verifyWith(chave)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public String extrairPapel(String token) {
        return Jwts.parser()
                .verifyWith(chave)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("papel", String.class);
    }

    public boolean tokenValido(String token) {
        try {
            Jwts.parser().verifyWith(chave).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}