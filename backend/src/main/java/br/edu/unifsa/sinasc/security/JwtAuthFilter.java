package br.edu.unifsa.sinasc.security;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import br.edu.unifsa.sinasc.usuario.UsuarioRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Lê "Authorization: Bearer <token>", valida o JWT e carrega o usuário do banco.
 * Token inválido, expirado ou de conta desativada: segue sem autenticação (a resposta será 401).
 * Não é um @Component de propósito: é criado no SecurityConfig para rodar uma única vez.
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String PREFIXO = "Bearer ";

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    public JwtAuthFilter(JwtService jwtService, UsuarioRepository usuarioRepository) {
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String cabecalho = request.getHeader("Authorization");

        if (cabecalho != null && cabecalho.startsWith(PREFIXO)) {
            try {
                Long id = jwtService.lerUsuarioId(cabecalho.substring(PREFIXO.length()).trim());
                usuarioRepository.findById(id)
                        .filter(u -> u.isAtivo())
                        .ifPresent(u -> {
                            AuthUser principal = new AuthUser(u.getId(), u.getEmail(), u.getPerfil());
                            UsernamePasswordAuthenticationToken autenticacao =
                                    new UsernamePasswordAuthenticationToken(
                                            principal,
                                            null,
                                            List.of(new SimpleGrantedAuthority("ROLE_" + u.getPerfil().name())));
                            SecurityContextHolder.getContext().setAuthentication(autenticacao);
                        });
            } catch (JwtException | IllegalArgumentException e) {
                // token inválido ou expirado: não autentica
            }
        }

        chain.doFilter(request, response);
    }
}
