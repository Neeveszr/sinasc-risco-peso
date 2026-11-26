package br.edu.unifsa.sinasc.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import br.edu.unifsa.sinasc.config.ValoresDeExemplo;
import br.edu.unifsa.sinasc.usuario.Perfil;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/** Gera e valida tokens JWT (assinatura HMAC). O segredo vem da variável JWT_SECRET. */
@Service
public class JwtService {

    private final SecretKey chave;
    private final long expiracaoMinutos;

    public JwtService(@Value("${app.jwt.secret}") String segredo,
                      @Value("${app.jwt.expiration-minutes}") long expiracaoMinutos) {
        if (ValoresDeExemplo.ehPlaceholder(segredo)) {
            throw new IllegalStateException(
                    "JWT_SECRET está com o valor de exemplo do .env.example; gere um segredo de verdade: openssl rand -hex 32");
        }
        byte[] bytes = segredo.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET precisa ter pelo menos 32 caracteres (gere um com: openssl rand -hex 32)");
        }
        this.chave = Keys.hmacShaKeyFor(bytes);
        this.expiracaoMinutos = expiracaoMinutos;
    }

    public String gerar(Long usuarioId, String email, Perfil perfil) {
        Instant agora = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(usuarioId))
                .claim("email", email)
                .claim("perfil", perfil.name())
                .issuedAt(Date.from(agora))
                .expiration(Date.from(agora.plus(expiracaoMinutos, ChronoUnit.MINUTES)))
                .signWith(chave)
                .compact();
    }

    /**
     * Valida assinatura e validade e devolve o id do usuário.
     * Lança JwtException (ou IllegalArgumentException) se o token for inválido ou estiver expirado.
     * O perfil NÃO é lido do token: o filtro consulta o banco, então mudar perfil ou desativar
     * a conta vale imediatamente.
     */
    public Long lerUsuarioId(String token) throws JwtException {
        String assunto = Jwts.parser()
                .verifyWith(chave)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
        return Long.valueOf(assunto);
    }

    public long getExpiracaoMinutos() {
        return expiracaoMinutos;
    }
}
