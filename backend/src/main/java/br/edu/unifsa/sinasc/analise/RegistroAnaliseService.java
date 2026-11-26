
package br.edu.unifsa.sinasc.analise;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import br.edu.unifsa.sinasc.exception.ConflitoException;
import br.edu.unifsa.sinasc.exception.RecursoNaoEncontradoException;
import br.edu.unifsa.sinasc.security.AuthUser;
import br.edu.unifsa.sinasc.usuario.Perfil;
import br.edu.unifsa.sinasc.usuario.Usuario;
import br.edu.unifsa.sinasc.usuario.UsuarioRepository;
import br.edu.unifsa.sinasc.analise.RegistroAnaliseDtos.RegistroRequest;

@Service
public class RegistroAnaliseService {

    private final RegistroAnaliseRepository registroRepository;
    private final PredicaoRepository predicaoRepository;
    private final UsuarioRepository usuarioRepository;

    public RegistroAnaliseService(
            RegistroAnaliseRepository registroRepository,
            PredicaoRepository predicaoRepository,
            UsuarioRepository usuarioRepository) {

        this.registroRepository = registroRepository;
        this.predicaoRepository = predicaoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public Page<RegistroAnalise> listar(AuthUser logado, Pageable pageable) {

        if (ehGestor(logado)) {
            return registroRepository.findAll(pageable);
        }

        return registroRepository.findByUsuarioId(logado.id(), pageable);
    }

    @Transactional(readOnly = true)
    public RegistroAnalise buscar(Long id, AuthUser logado) {

        RegistroAnalise registro = registroRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Registro de análise " + id + " não encontrado"));

        verificarAcesso(registro, logado);

        return registro;
    }

    @Transactional
    public RegistroAnalise criar(RegistroRequest dados, AuthUser logado) {

        validarPartos(dados);

        Usuario usuario = usuarioRepository.findById(logado.id())
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Usuário não encontrado"));

        RegistroAnalise registro = new RegistroAnalise();

        registro.setUsuario(usuario);
        preencherDados(registro, dados);

        return registroRepository.save(registro);
    }

    @Transactional
    public RegistroAnalise atualizar(
            Long id,
            RegistroRequest dados,
            AuthUser logado) {

        RegistroAnalise registro = buscar(id, logado);

        validarPartos(dados);
        preencherDados(registro, dados);

        return registroRepository.save(registro);
    }

    @Transactional
    public void excluir(Long id, AuthUser logado) {

        RegistroAnalise registro = buscar(id, logado);

        if (predicaoRepository.existsByRegistroAnaliseId(id)) {
            throw new ConflitoException(
                    "Não é possível excluir um registro que possui predições associadas");
        }

        registroRepository.delete(registro);
    }

    private void verificarAcesso(
            RegistroAnalise registro,
            AuthUser logado) {

        if (!ehGestor(logado)
                && !registro.getUsuario().getId().equals(logado.id())) {

            throw new RecursoNaoEncontradoException(
                    "Registro de análise não encontrado");
        }
    }

    private boolean ehGestor(AuthUser logado) {
        return logado.perfil() == Perfil.GESTOR_AUDITOR;
    }

    private void validarPartos(RegistroRequest dados) {

        if (dados.gestacoesAnteriores() == null
                || dados.partosNormais() == null
                || dados.partosCesareos() == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Os campos de gestações e partos são obrigatórios");
        }

        if (dados.partosNormais() + dados.partosCesareos()
                > dados.gestacoesAnteriores()) {

            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "A soma de partos normais e cesáreos não pode ultrapassar as gestações anteriores");
        }
    }

    private void preencherDados(
            RegistroAnalise registro,
            RegistroRequest dados) {

        registro.setFaixaEtaria(dados.faixaEtaria());
        registro.setEstadoCivil(dados.estadoCivil());
        registro.setEscolaridade(dados.escolaridade());
        registro.setTipoGravidez(dados.tipoGravidez());
        registro.setGestacoesAnteriores(dados.gestacoesAnteriores());
        registro.setPartosNormais(dados.partosNormais());
        registro.setPartosCesareos(dados.partosCesareos());
        registro.setFilhosVivos(dados.filhosVivos());
        registro.setPerdasFetais(dados.perdasFetais());
        registro.setMesPreNatal(dados.mesPreNatal());
    }
}
