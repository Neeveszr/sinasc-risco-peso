package br.edu.unifsa.sinasc.auditoria;

import java.time.Instant;

public record LogAcessoResponse(
        Long id,
        Long usuarioId,
        String usuarioEmail,
        String acao,
        String recurso,
        Instant criadoEm) {

    public static LogAcessoResponse de(LogAcesso log) {
        return new LogAcessoResponse(
                log.getId(),
                log.getUsuario().getId(),
                log.getUsuario().getEmail(),
                log.getAcao(),
                log.getRecurso(),
                log.getCriadoEm());
    }
}
