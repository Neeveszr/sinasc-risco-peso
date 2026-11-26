package br.edu.unifsa.sinasc.usuario;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import br.edu.unifsa.sinasc.auditoria.AuditoriaService;
import br.edu.unifsa.sinasc.exception.CredenciaisInvalidasException;
import br.edu.unifsa.sinasc.security.JwtService;
import br.edu.unifsa.sinasc.usuario.AuthDtos.TokenResponse;
import br.edu.unifsa.sinasc.usuario.UsuarioDtos.UsuarioResponse;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditoriaService auditoriaService;

    /** Hash de uma senha qualquer, usado para gastar o mesmo tempo quando o e-mail não existe. */
    private final String hashFalso;

    public AuthService(UsuarioRepository usuarioRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuditoriaService auditoriaService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.auditoriaService = auditoriaService;
        this.hashFalso = passwordEncoder.encode("senha-falsa-para-igualar-tempo");
    }

    /**
     * Login. Sucesso e falhas ficam no registro de acessos. Para quem chama, toda falha é a mesma
     * resposta (401, "E-mail ou senha inválidos"); o motivo real só aparece na auditoria.
     */
    public TokenResponse login(String email, String senha) {
        String emailInformado = email.trim();
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(emailInformado).orElse(null);

        String hash = usuario != null ? usuario.getSenhaHash() : hashFalso;
        boolean senhaConfere = passwordEncoder.matches(senha, hash);

        if (usuario == null) {
            auditoriaService.registrarFalhaLogin(null, emailInformado, "email-desconhecido");
            throw new CredenciaisInvalidasException();
        }
        if (!senhaConfere) {
            auditoriaService.registrarFalhaLogin(usuario.getId(), emailInformado, "senha-incorreta");
            throw new CredenciaisInvalidasException();
        }
        if (!usuario.isAtivo()) {
            auditoriaService.registrarFalhaLogin(usuario.getId(), emailInformado, "conta-inativa");
            throw new CredenciaisInvalidasException();
        }

        auditoriaService.registrar(usuario.getId(), AuditoriaService.ACAO_LOGIN, "auth");

        String token = jwtService.gerar(usuario.getId(), usuario.getEmail(), usuario.getPerfil());
        return new TokenResponse(token, "Bearer", jwtService.getExpiracaoMinutos(), UsuarioResponse.de(usuario));
    }
}
