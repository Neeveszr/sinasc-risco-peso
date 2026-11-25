package br.edu.unifsa.sinasc.auditoria;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.edu.unifsa.sinasc.security.Permissoes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/auditoria")
@Tag(name = "Auditoria", description = "Registro de acessos (somente gestor/auditor)")
@SecurityRequirement(name = "bearerAuth")
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    public AuditoriaController(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @Operation(summary = "Lista os acessos registrados", description = "Filtro opcional por usuarioId")
    @GetMapping("/acessos")
    @PreAuthorize(Permissoes.GESTOR_AUDITOR)
    public Page<LogAcessoResponse> acessos(
            @RequestParam(required = false) Long usuarioId,
            @PageableDefault(size = 20, sort = "criadoEm", direction = Sort.Direction.DESC) Pageable pageable) {
        return auditoriaService.listar(usuarioId, pageable);
    }
}
