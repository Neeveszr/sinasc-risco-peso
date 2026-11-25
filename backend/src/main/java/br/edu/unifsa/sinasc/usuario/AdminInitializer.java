package br.edu.unifsa.sinasc.usuario;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Cria o primeiro gestor/auditor se ADMIN_EMAIL e ADMIN_PASSWORD estiverem definidos.
 * Sem isso ninguém conseguiria criar gestores, já que o cadastro público só gera operadores.
 */
@Component
public class AdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);

    private final UsuarioService usuarioService;
    private final UsuarioRepository usuarioRepository;
    private final String email;
    private final String senha;

    public AdminInitializer(UsuarioService usuarioService,
                            UsuarioRepository usuarioRepository,
                            @Value("${app.admin.email:}") String email,
                            @Value("${app.admin.password:}") String senha) {
        this.usuarioService = usuarioService;
        this.usuarioRepository = usuarioRepository;
        this.email = email;
        this.senha = senha;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email.isBlank() || senha.isBlank()) {
            log.info("ADMIN_EMAIL/ADMIN_PASSWORD não definidos: nenhum gestor inicial será criado.");
            return;
        }
        if (senha.length() < 8 || senha.length() > 72) {
            log.warn("ADMIN_PASSWORD deve ter entre 8 e 72 caracteres: gestor inicial não criado.");
            return;
        }
        if (usuarioRepository.existsByEmailIgnoreCase(email.trim())) {
            return;
        }
        usuarioService.criar("Administrador", email, senha, Perfil.GESTOR_AUDITOR);
        log.info("Gestor/auditor inicial criado: {}", email.trim());
    }
}
