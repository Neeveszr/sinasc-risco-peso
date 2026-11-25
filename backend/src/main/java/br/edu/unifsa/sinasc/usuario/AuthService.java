package br.edu.unifsa.sinasc.usuario;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import br.edu.unifsa.sinasc.auditoria.AuditoriaService;
import br.edu.unifsa.sinasc.exception.CredenciaisInvalidasException;
import br.edu.unifsa.sinasc.security.JwtService;
import br.edu.unifsa.sinasc.usuario.AuthDtos.CadastroRequest;
import br.edu.unifsa.sinasc.usuario.AuthDtos.TokenResponse;
import br.edu.unifsa.sinasc.usuario.UsuarioDtos.UsuarioResponse;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioService usuarioService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditoriaService auditoriaService;

    /** Hash de uma senha qualquer, usado para gastar o mesmo tempo quando o e-mail não existe. */
    private final String hashFalso;

    public AuthService(UsuarioRepository usuarioRepository,
                       UsuarioService usuarioService,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuditoriaService auditoriaService) {
        this.usuarioRepository = usuarioRepository;
        this.usuarioService = usuarioService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.auditoriaService = auditoriaService;
        this.hashFalso = passwordEncoder.encode("senha-falsa-para-igualar-tempo");
    }

    public TokenResponse login(String email, String senha) {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email.trim()).orElse(null);

        String hash = usuario != null ? usuario.getSenhaHash() : hashFalso;
        boolean senhaConfere = passwordEncoder.matches(senha, hash);

        if (usuario == null || !senhaConfere || !usuario.isAtivo()) {
            throw new CredenciaisInvalidasException();
        }

        auditoriaService.registrar(usuario.getId(), "LOGIN", "auth");

        String token = jwtService.gerar(usuario.getId(), usuario.getEmail(), usuario.getPerfil());
        return new TokenResponse(token, "Bearer", jwtService.getExpiracaoMinutos(), UsuarioResponse.de(usuario));
    }

    /** Cadastro público: sempre OPERADOR. Gestor/auditor só é criado por outro gestor. */
    public UsuarioResponse cadastrar(CadastroRequest dados) {
        Usuario criado = usuarioService.criar(dados.nome(), dados.email(), dados.senha(), Perfil.OPERADOR);
        return UsuarioResponse.de(criado);
    }
}
