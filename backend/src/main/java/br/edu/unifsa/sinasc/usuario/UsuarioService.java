package br.edu.unifsa.sinasc.usuario;

import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.unifsa.sinasc.exception.ConflitoException;
import br.edu.unifsa.sinasc.exception.RecursoNaoEncontradoException;
import br.edu.unifsa.sinasc.security.AuthUser;
import br.edu.unifsa.sinasc.usuario.UsuarioDtos.UsuarioAtualizarRequest;

@Service
public class UsuarioService {

    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Usuario criar(String nome, String email, String senha, Perfil perfil) {
        String emailNormalizado = normalizar(email);
        if (repository.existsByEmailIgnoreCase(emailNormalizado)) {
            throw new ConflitoException("Já existe um usuário com este e-mail");
        }
        Usuario usuario = new Usuario();
        usuario.setNome(nome.trim());
        usuario.setEmail(emailNormalizado);
        usuario.setSenhaHash(passwordEncoder.encode(senha));
        usuario.setPerfil(perfil);
        usuario.setAtivo(true);
        return repository.save(usuario);
    }

    @Transactional(readOnly = true)
    public Page<Usuario> listar(Pageable pageable) {
        return repository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Usuario buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário " + id + " não encontrado"));
    }

    @Transactional
    public Usuario atualizar(Long id, UsuarioAtualizarRequest dados, AuthUser logado) {
        Usuario usuario = buscar(id);

        String novoEmail = normalizar(dados.email());
        if (!novoEmail.equalsIgnoreCase(usuario.getEmail()) && repository.existsByEmailIgnoreCase(novoEmail)) {
            throw new ConflitoException("Já existe um usuário com este e-mail");
        }
        if (!dados.ativo() && usuario.getId().equals(logado.id())) {
            throw new ConflitoException("Você não pode desativar a própria conta");
        }

        usuario.setNome(dados.nome().trim());
        usuario.setEmail(novoEmail);
        usuario.setAtivo(dados.ativo());
        if (dados.senha() != null) {
            usuario.setSenhaHash(passwordEncoder.encode(dados.senha()));
        }
        return repository.save(usuario);
    }

    /**
     * Só um gestor/auditor chega aqui (ver controller). Como ele não pode rebaixar a si mesmo,
     * sempre sobra pelo menos um gestor ativo.
     */
    @Transactional
    public Usuario alterarPerfil(Long id, Perfil novoPerfil, AuthUser logado) {
        Usuario usuario = buscar(id);
        if (usuario.getId().equals(logado.id()) && usuario.getPerfil() != novoPerfil) {
            throw new ConflitoException("Você não pode alterar o próprio perfil");
        }
        usuario.setPerfil(novoPerfil);
        return repository.save(usuario);
    }

    /** DELETE é lógico: a conta fica inativa, porque o histórico e a auditoria referenciam o usuário. */
    @Transactional
    public void desativar(Long id, AuthUser logado) {
        Usuario usuario = buscar(id);
        if (usuario.getId().equals(logado.id())) {
            throw new ConflitoException("Você não pode desativar a própria conta");
        }
        usuario.setAtivo(false);
        repository.save(usuario);
    }

    private static String normalizar(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
