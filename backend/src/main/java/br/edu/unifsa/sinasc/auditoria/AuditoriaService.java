package br.edu.unifsa.sinasc.auditoria;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import br.edu.unifsa.sinasc.usuario.UsuarioRepository;
import jakarta.persistence.criteria.Predicate;

/**
 * Registra e consulta acessos. Quem precisar auditar uma ação (ex.: Carlos, ao gerar ou consultar
 * uma predição) chama:
 * <pre>
 * auditoriaService.registrar(logado.id(), "PREDICAO_CRIAR", "predicao/" + id);
 * </pre>
 * Nunca coloque dados pessoais ou clínicos no texto de "recurso".
 */
@Service
public class AuditoriaService {

    public static final String ACAO_LOGIN = "LOGIN";
    public static final String ACAO_LOGIN_FALHA = "LOGIN_FALHA";

    private static final int MAX_ACAO = 40;
    private static final int MAX_RECURSO = 120;
    private static final int MAX_IDENTIFICADOR = 160;

    private final LogAcessoRepository repository;
    private final UsuarioRepository usuarioRepository;

    public AuditoriaService(LogAcessoRepository repository, UsuarioRepository usuarioRepository) {
        this.repository = repository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public void registrar(Long usuarioId, String acao, String recurso) {
        LogAcesso log = novo(acao, recurso);
        log.setUsuario(usuarioRepository.getReferenceById(usuarioId));
        repository.save(log);
    }

    /**
     * Falha de login. Se o e-mail existe, vincula ao usuário; se não existe, guarda só o e-mail informado.
     * Usa transação própria para o registro ser gravado independentemente de quem chamou.
     *
     * @param motivo curto e sem dados sensíveis (ex.: "senha-incorreta"); nunca a senha digitada
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarFalhaLogin(Long usuarioId, String emailInformado, String motivo) {
        LogAcesso log = novo(ACAO_LOGIN_FALHA, "auth/" + motivo);
        if (usuarioId != null) {
            log.setUsuario(usuarioRepository.getReferenceById(usuarioId));
        }
        log.setIdentificador(cortar(emailInformado.trim(), MAX_IDENTIFICADOR));
        repository.save(log);
    }

    /** Filtros opcionais: usuário e ação (ex.: LOGIN_FALHA). */
    @Transactional(readOnly = true)
    public Page<LogAcessoResponse> listar(Long usuarioId, String acao, Pageable pageable) {
        Specification<LogAcesso> filtro = (root, query, cb) -> {
            List<Predicate> condicoes = new ArrayList<>();
            if (usuarioId != null) {
                condicoes.add(cb.equal(root.get("usuario").get("id"), usuarioId));
            }
            if (acao != null && !acao.isBlank()) {
                condicoes.add(cb.equal(root.get("acao"), acao.trim()));
            }
            return cb.and(condicoes.toArray(new Predicate[0]));
        };
        return repository.findAll(filtro, pageable).map(LogAcessoResponse::de);
    }

    private static LogAcesso novo(String acao, String recurso) {
        LogAcesso log = new LogAcesso();
        log.setAcao(cortar(acao, MAX_ACAO));
        log.setRecurso(cortar(recurso, MAX_RECURSO));
        return log;
    }

    private static String cortar(String texto, int max) {
        return texto.length() <= max ? texto : texto.substring(0, max);
    }
}
