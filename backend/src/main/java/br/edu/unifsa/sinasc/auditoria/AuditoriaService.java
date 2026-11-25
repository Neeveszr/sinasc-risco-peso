package br.edu.unifsa.sinasc.auditoria;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.unifsa.sinasc.usuario.UsuarioRepository;

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

    private static final int MAX_ACAO = 40;
    private static final int MAX_RECURSO = 120;

    private final LogAcessoRepository repository;
    private final UsuarioRepository usuarioRepository;

    public AuditoriaService(LogAcessoRepository repository, UsuarioRepository usuarioRepository) {
        this.repository = repository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public void registrar(Long usuarioId, String acao, String recurso) {
        LogAcesso log = new LogAcesso();
        log.setUsuario(usuarioRepository.getReferenceById(usuarioId));
        log.setAcao(cortar(acao, MAX_ACAO));
        log.setRecurso(cortar(recurso, MAX_RECURSO));
        repository.save(log);
    }

    @Transactional(readOnly = true)
    public Page<LogAcessoResponse> listar(Long usuarioId, Pageable pageable) {
        Page<LogAcesso> pagina = (usuarioId == null)
                ? repository.findAll(pageable)
                : repository.findByUsuarioId(usuarioId, pageable);
        return pagina.map(LogAcessoResponse::de);
    }

    private static String cortar(String texto, int max) {
        return texto.length() <= max ? texto : texto.substring(0, max);
    }
}
