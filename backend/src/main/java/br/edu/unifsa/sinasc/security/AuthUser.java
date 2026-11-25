package br.edu.unifsa.sinasc.security;

import br.edu.unifsa.sinasc.usuario.Perfil;

/**
 * Usuário autenticado na requisição atual. Qualquer controller pode recebê-lo assim:
 * <pre>
 * public X metodo(@AuthenticationPrincipal AuthUser logado) { ... logado.id() ... }
 * </pre>
 */
public record AuthUser(Long id, String email, Perfil perfil) {
}
