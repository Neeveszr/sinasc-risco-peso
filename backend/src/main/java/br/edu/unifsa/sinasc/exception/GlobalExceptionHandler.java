package br.edu.unifsa.sinasc.exception;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Erros padronizados no formato RFC 7807 (application/problem+json):
 * title, status, detail (e "erros" nas validações).
 * Quem criar exceções novas (ex.: serviço do modelo fora do ar) só adiciona um método aqui.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ProblemDetail naoEncontrado(RecursoNaoEncontradoException ex) {
        return problema(HttpStatus.NOT_FOUND, "Recurso não encontrado", ex.getMessage());
    }

    @ExceptionHandler(ConflitoException.class)
    public ProblemDetail conflito(ConflitoException ex) {
        return problema(HttpStatus.CONFLICT, "Conflito", ex.getMessage());
    }

    /** Rede de segurança para duas requisições simultâneas com o mesmo e-mail. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail integridade(DataIntegrityViolationException ex) {
        log.warn("Violação de integridade: {}", ex.getMostSpecificCause().getMessage());
        return problema(HttpStatus.CONFLICT, "Conflito", "Operação viola uma regra de integridade dos dados");
    }

    @ExceptionHandler(CredenciaisInvalidasException.class)
    public ProblemDetail credenciais(CredenciaisInvalidasException ex) {
        return problema(HttpStatus.UNAUTHORIZED, "Não autenticado", ex.getMessage());
    }

    /** Lançada pelo @PreAuthorize quando o perfil não tem permissão. */
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail acessoNegado(AccessDeniedException ex) {
        return problema(HttpStatus.FORBIDDEN, "Acesso negado", "Seu perfil não tem permissão para esta operação");
    }

    /** Serviço de IA fora do ar, com timeout ou com resposta fora do contrato: HTTP 503. */
    @ExceptionHandler(ModeloIndisponivelException.class)
    public ProblemDetail modeloIndisponivel(ModeloIndisponivelException ex) {
        log.warn("Serviço de IA indisponível: {}", ex.getMessage());
        return problema(HttpStatus.SERVICE_UNAVAILABLE, "Serviço de IA indisponível",
                "O serviço de IA não está disponível no momento. Tente novamente em instantes.");
    }

    /** O serviço de IA rejeitou os dados (422 dele): devolve 422 no nosso formato, com os campos em camelCase. */
    @ExceptionHandler(ModeloEntradaInvalidaException.class)
    public ProblemDetail modeloRejeitouEntrada(ModeloEntradaInvalidaException ex) {
        List<Map<String, String>> erros = ex.getErros().stream()
                .map(e -> Map.of("campo", e.campo(), "mensagem", e.mensagem()))
                .toList();
        ProblemDetail pd = problema(HttpStatus.UNPROCESSABLE_ENTITY, "Dados rejeitados pelo modelo",
                "O serviço de IA rejeitou os dados enviados");
        pd.setProperty("erros", erros);
        return pd;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail inesperado(Exception ex) {
        log.error("Erro inesperado", ex);
        return problema(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno", "Ocorreu um erro inesperado");
    }

    /** Falha de validação (Bean Validation): HTTP 422 com a lista de campos inválidos. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<Map<String, String>> erros = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> Map.of(
                        "campo", e.getField(),
                        "mensagem", e.getDefaultMessage() == null ? "valor inválido" : e.getDefaultMessage()))
                .toList();
        ProblemDetail pd = problema(HttpStatus.UNPROCESSABLE_ENTITY, "Validação falhou", "Dados de entrada inválidos");
        pd.setProperty("erros", erros);
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(pd);
    }

    private static ProblemDetail problema(HttpStatus status, String titulo, String detalhe) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detalhe);
        pd.setTitle(titulo);
        return pd;
    }
}
