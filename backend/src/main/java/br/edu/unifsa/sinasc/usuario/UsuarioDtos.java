package br.edu.unifsa.sinasc.usuario;

import java.time.Instant;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Entradas e saídas de /api/usuarios. A senha (ou o hash) nunca aparece nas respostas. */
public final class UsuarioDtos {

    private UsuarioDtos() {
    }

    /** Criação feita por um gestor/auditor, que escolhe o perfil. */
    public record UsuarioCriarRequest(
            @NotBlank(message = "nome é obrigatório")
            @Size(max = 120, message = "nome deve ter no máximo 120 caracteres")
            String nome,

            @NotBlank(message = "e-mail é obrigatório")
            @Email(message = "e-mail inválido")
            @Size(max = 160, message = "e-mail deve ter no máximo 160 caracteres")
            String email,

            @NotBlank(message = "senha é obrigatória")
            @Size(min = 8, max = 72, message = "senha deve ter entre 8 e 72 caracteres")
            String senha,

            @NotNull(message = "perfil é obrigatório (OPERADOR ou GESTOR_AUDITOR)")
            Perfil perfil) {
    }

    /**
     * Atualização completa (PUT). A senha é opcional: se for omitida (null), não muda.
     * Se for enviada, precisa ter de 8 a 72 caracteres e não pode ser só espaços;
     * string vazia ("") é rejeitada com 422, então o front deve omitir o campo para não trocar a senha.
     */
    public record UsuarioAtualizarRequest(
            @NotBlank(message = "nome é obrigatório")
            @Size(max = 120, message = "nome deve ter no máximo 120 caracteres")
            String nome,

            @NotBlank(message = "e-mail é obrigatório")
            @Email(message = "e-mail inválido")
            @Size(max = 160, message = "e-mail deve ter no máximo 160 caracteres")
            String email,

            @Size(min = 8, max = 72, message = "senha deve ter entre 8 e 72 caracteres")
            @Pattern(regexp = ".*\\S.*", message = "senha não pode ser só espaços")
            String senha,

            @NotNull(message = "ativo é obrigatório")
            Boolean ativo) {
    }

    public record PerfilRequest(
            @NotNull(message = "perfil é obrigatório (OPERADOR ou GESTOR_AUDITOR)")
            Perfil perfil) {
    }

    public record UsuarioResponse(
            Long id,
            String nome,
            String email,
            Perfil perfil,
            boolean ativo,
            Instant criadoEm) {

        public static UsuarioResponse de(Usuario u) {
            return new UsuarioResponse(
                    u.getId(), u.getNome(), u.getEmail(), u.getPerfil(), u.isAtivo(), u.getCriadoEm());
        }
    }
}
