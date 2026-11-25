package br.edu.unifsa.sinasc.usuario;

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
import br.edu.unifsa.sinasc.security.AuthUser;
import br.edu.unifsa.sinasc.security.Permissoes;
import br.edu.unifsa.sinasc.usuario.UsuarioDtos.PerfilRequest;
import br.edu.unifsa.sinasc.usuario.UsuarioDtos.UsuarioAtualizarRequest;
import br.edu.unifsa.sinasc.usuario.UsuarioDtos.UsuarioCriarRequest;
import br.edu.unifsa.sinasc.usuario.UsuarioDtos.UsuarioResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/usuarios")
@Tag(name = "Usuários", description = "Gestão de usuários (gestor/auditor) e consulta da própria conta")
@SecurityRequirement(name = "bearerAuth")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final AuditoriaService auditoriaService;

    public UsuarioController(UsuarioService usuarioService, AuditoriaService auditoriaService) {
        this.usuarioService = usuarioService;
        this.auditoriaService = auditoriaService;
    }

    @Operation(summary = "Dados do usuário logado")
    @GetMapping("/eu")
    @PreAuthorize(Permissoes.QUALQUER_PERFIL)
    public UsuarioResponse eu(@AuthenticationPrincipal AuthUser logado) {
        return UsuarioResponse.de(usuarioService.buscar(logado.id()));
    }

    @Operation(summary = "Lista usuários (paginado)")
    @GetMapping
    @PreAuthorize(Permissoes.GESTOR_AUDITOR)
    public Page<UsuarioResponse> listar(
            @PageableDefault(size = 20, sort = "criadoEm", direction = Sort.Direction.DESC) Pageable pageable) {
        return usuarioService.listar(pageable).map(UsuarioResponse::de);
    }

    @Operation(summary = "Busca um usuário pelo id")
    @GetMapping("/{id}")
    @PreAuthorize(Permissoes.GESTOR_AUDITOR)
    public UsuarioResponse buscar(@PathVariable Long id) {
        return UsuarioResponse.de(usuarioService.buscar(id));
    }

    @Operation(summary = "Cria um usuário escolhendo o perfil")
    @PostMapping
    @PreAuthorize(Permissoes.GESTOR_AUDITOR)
    public ResponseEntity<UsuarioResponse> criar(@Valid @RequestBody UsuarioCriarRequest dados,
                                                 @AuthenticationPrincipal AuthUser logado) {
        Usuario criado = usuarioService.criar(dados.nome(), dados.email(), dados.senha(), dados.perfil());
        auditoriaService.registrar(logado.id(), "USUARIO_CRIAR", "usuario/" + criado.getId());

        URI local = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(criado.getId())
                .toUri();
        return ResponseEntity.created(local).body(UsuarioResponse.de(criado));
    }

    @Operation(summary = "Atualiza nome, e-mail, situação e (opcionalmente) a senha")
    @PutMapping("/{id}")
    @PreAuthorize(Permissoes.GESTOR_AUDITOR)
    public UsuarioResponse atualizar(@PathVariable Long id,
                                     @Valid @RequestBody UsuarioAtualizarRequest dados,
                                     @AuthenticationPrincipal AuthUser logado) {
        Usuario atualizado = usuarioService.atualizar(id, dados, logado);
        auditoriaService.registrar(logado.id(), "USUARIO_ATUALIZAR", "usuario/" + id);
        return UsuarioResponse.de(atualizado);
    }

    @Operation(summary = "Altera o perfil de um usuário")
    @PutMapping("/{id}/perfil")
    @PreAuthorize(Permissoes.GESTOR_AUDITOR)
    public UsuarioResponse alterarPerfil(@PathVariable Long id,
                                         @Valid @RequestBody PerfilRequest dados,
                                         @AuthenticationPrincipal AuthUser logado) {
        Usuario atualizado = usuarioService.alterarPerfil(id, dados.perfil(), logado);
        auditoriaService.registrar(logado.id(), "USUARIO_PERFIL", "usuario/" + id);
        return UsuarioResponse.de(atualizado);
    }

    @Operation(summary = "Desativa um usuário", description = "Exclusão lógica: a conta fica inativa e não consegue mais entrar")
    @DeleteMapping("/{id}")
    @PreAuthorize(Permissoes.GESTOR_AUDITOR)
    public ResponseEntity<Void> desativar(@PathVariable Long id, @AuthenticationPrincipal AuthUser logado) {
        usuarioService.desativar(id, logado);
        auditoriaService.registrar(logado.id(), "USUARIO_DESATIVAR", "usuario/" + id);
        return ResponseEntity.noContent().build();
    }
}
