package br.edu.unifsa.sinasc.usuario;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Entradas e saídas de /api/auth. */
public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(
            @NotBlank(message = "e-mail é obrigatório")
            @Email(message = "e-mail inválido")
            @Size(max = 160, message = "e-mail deve ter no máximo 160 caracteres")
            String email,

            @NotBlank(message = "senha é obrigatória")
            @Size(max = 72, message = "senha deve ter no máximo 72 caracteres")
            String senha) {
    }

    public record TokenResponse(
            String token,
            String tipo,
            long expiraEmMinutos,
            UsuarioDtos.UsuarioResponse usuario) {
    }
}
