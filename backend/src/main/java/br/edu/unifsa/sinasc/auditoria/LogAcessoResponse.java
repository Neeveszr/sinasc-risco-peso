package br.edu.unifsa.sinasc.auditoria;

import java.time.Instant;

import br.edu.unifsa.sinasc.usuario.Usuario;

/**
 * usuarioId e usuarioEmail ficam nulos quando a tentativa foi com um e-mail que não existe;
 * nesse caso "identificador" mostra o e-mail informado.
 */
public record LogAcessoResponse(
        Long id,
        Long usuarioId,
        String usuarioEmail,
        String identificador,
        String acao,
        String recurso,
        Instant criadoEm) {

    public static LogAcessoResponse de(LogAcesso log) {
        Usuario usuario = log.getUsuario();
        return new LogAcessoResponse(
                log.getId(),
                usuario == null ? null : usuario.getId(),
                usuario == null ? null : usuario.getEmail(),
                log.getIdentificador(),
                log.getAcao(),
                log.getRecurso(),
                log.getCriadoEm());
    }
}
