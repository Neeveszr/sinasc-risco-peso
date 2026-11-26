package br.edu.unifsa.sinasc.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import br.edu.unifsa.sinasc.usuario.Perfil;
import io.jsonwebtoken.JwtException;

class JwtServiceTest {

    private static final String SEGREDO = "segredo-de-teste-com-bem-mais-de-32-caracteres";
    private static final String OUTRO_SEGREDO = "outro-segredo-de-teste-com-mais-de-32-caracteres";

    @Test
    void geraTokenELeOIdDoUsuario() {
        JwtService jwt = new JwtService(SEGREDO, 5);

        String token = jwt.gerar(42L, "ana@exemplo.com", Perfil.OPERADOR);

        assertEquals(Long.valueOf(42L), jwt.lerUsuarioId(token));
    }

    @Test
    void rejeitaTokenAssinadoComOutraChave() {
        String token = new JwtService(OUTRO_SEGREDO, 5).gerar(1L, "a@b.com", Perfil.OPERADOR);

        assertThrows(JwtException.class, () -> new JwtService(SEGREDO, 5).lerUsuarioId(token));
    }

    @Test
    void rejeitaTokenExpirado() {
        JwtService jwt = new JwtService(SEGREDO, -1);
        String token = jwt.gerar(1L, "a@b.com", Perfil.OPERADOR);

        assertThrows(JwtException.class, () -> jwt.lerUsuarioId(token));
    }

    @Test
    void rejeitaSegredoDeExemploDoEnvExample() {
        String exemplo = "troque-por-um-segredo-aleatorio-com-pelo-menos-32-caracteres";

        assertThrows(IllegalStateException.class, () -> new JwtService(exemplo, 5));
    }

    @Test
    void rejeitaSegredoVazio() {
        assertThrows(IllegalStateException.class, () -> new JwtService("", 5));
    }

    @Test
    void rejeitaSegredoCurto() {
        assertThrows(IllegalStateException.class, () -> new JwtService("curto", 5));
    }
}
