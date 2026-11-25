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

    /** Cadastro público: não aceita "perfil"; a conta nasce sempre como OPERADOR. */
    public record CadastroRequest(
            @NotBlank(message = "nome é obrigatório")
            @Size(max = 120, message = "nome deve ter no máximo 120 caracteres")
            String nome,

            @NotBlank(message = "e-mail é obrigatório")
            @Email(message = "e-mail inválido")
            @Size(max = 160, message = "e-mail deve ter no máximo 160 caracteres")
            String email,

            @NotBlank(message = "senha é obrigatória")
            @Size(min = 8, max = 72, message = "senha deve ter entre 8 e 72 caracteres")
            String senha) {
    }

    public record TokenResponse(
            String token,
            String tipo,
            long expiraEmMinutos,
            UsuarioDtos.UsuarioResponse usuario) {
    }
}
