
package br.edu.unifsa.sinasc.analise;

import java.net.URI;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import br.edu.unifsa.sinasc.auditoria.AuditoriaService;
import br.edu.unifsa.sinasc.analise.RegistroAnaliseDtos.RegistroRequest;
import br.edu.unifsa.sinasc.analise.RegistroAnaliseDtos.RegistroResponse;
import br.edu.unifsa.sinasc.security.AuthUser;
import br.edu.unifsa.sinasc.security.Permissoes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/registros")
@Tag(name = "Registros de análise", description = "Gerenciamento dos registros de análise SINASC")
@SecurityRequirement(name = "bearerAuth")
public class RegistroAnaliseController {

    private final RegistroAnaliseService registroService;
    private final AuditoriaService auditoriaService;

    public RegistroAnaliseController(
            RegistroAnaliseService registroService,
            AuditoriaService auditoriaService) {
        this.registroService = registroService;
        this.auditoriaService = auditoriaService;
    }

    @Operation(summary = "Lista registros de análise")
    @GetMapping
    @PreAuthorize(Permissoes.QUALQUER_PERFIL)
    public Page<RegistroResponse> listar(
            @AuthenticationPrincipal AuthUser logado,
            @PageableDefault(size = 20, sort = "criadoEm", direction = Sort.Direction.DESC)
            Pageable pageable) {

        return registroService.listar(logado, pageable)
                .map(RegistroResponse::de);
    }

    @Operation(summary = "Busca um registro de análise pelo ID")
    @GetMapping("/{id}")
    @PreAuthorize(Permissoes.QUALQUER_PERFIL)
    public RegistroResponse buscar(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthUser logado) {

        return RegistroResponse.de(registroService.buscar(id, logado));
    }

    @Operation(summary = "Cria um registro de análise")
    @PostMapping
    @PreAuthorize(Permissoes.QUALQUER_PERFIL)
    public ResponseEntity<RegistroResponse> criar(
            @Valid @RequestBody RegistroRequest dados,
            @AuthenticationPrincipal AuthUser logado) {

        RegistroAnalise criado = registroService.criar(dados, logado);

        auditoriaService.registrar(
                logado.id(),
                "REGISTRO_CRIAR",
                "registro_analise/" + criado.getId());

        URI local = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(criado.getId())
                .toUri();

        return ResponseEntity.created(local)
                .body(RegistroResponse.de(criado));
    }

    @Operation(summary = "Atualiza um registro de análise")
    @PutMapping("/{id}")
    @PreAuthorize(Permissoes.QUALQUER_PERFIL)
    public RegistroResponse atualizar(
            @PathVariable Long id,
            @Valid @RequestBody RegistroRequest dados,
            @AuthenticationPrincipal AuthUser logado) {

        RegistroAnalise atualizado =
                registroService.atualizar(id, dados, logado);

        auditoriaService.registrar(
                logado.id(),
                "REGISTRO_ATUALIZAR",
                "registro_analise/" + id);

        return RegistroResponse.de(atualizado);
    }

    @Operation(summary = "Exclui um registro de análise")
    @DeleteMapping("/{id}")
    @PreAuthorize(Permissoes.QUALQUER_PERFIL)
    public ResponseEntity<Void> excluir(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthUser logado) {

        registroService.excluir(id, logado);

        auditoriaService.registrar(
                logado.id(),
                "REGISTRO_EXCLUIR",
                "registro_analise/" + id);

        return ResponseEntity.noContent().build();
    }
}
