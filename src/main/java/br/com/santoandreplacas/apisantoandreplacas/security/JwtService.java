package br.com.santoandreplacas.apisantoandreplacas.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService {

    // Em produção, essa chave viria de uma variável de ambiente, nunca fixa no código
    private final SecretKey chave = Keys.hmacShaKeyFor(
            "chave-secreta-temporaria-para-estudo-trocar-depois-1234567890".getBytes()
    );

    private final long expiracaoMs = 3600_000; // 1 hora

    public String gerarToken(String email, String papel) {
        Date agora = new Date();
        Date expiracao = new Date(agora.getTime() + expiracaoMs);

        return Jwts.builder()
                .subject(email)
                .claim("papel", papel)
                .issuedAt(agora)
                .expiration(expiracao)
                .signWith(chave)
                .compact();
    }

    public String extrairEmail(String token) {
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