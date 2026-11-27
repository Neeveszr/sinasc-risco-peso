package br.edu.unifsa.sinasc.exception;

import java.util.List;

/**
 * O serviço de IA rejeitou os dados enviados (HTTP 422 dele). Como a API valida a entrada antes,
 * isso indica uma regra que o serviço tem e a API ainda não. Vira HTTP 422 no formato padrão,
 * com os campos já traduzidos para os nomes do front (camelCase).
 */
public class ModeloEntradaInvalidaException extends RuntimeException {

    private final List<ErroCampo> erros;

    public ModeloEntradaInvalidaException(List<ErroCampo> erros) {
        super("Dados rejeitados pelo serviço de IA");
        this.erros = List.copyOf(erros);
    }

    public List<ErroCampo> getErros() {
        return erros;
    }

    public record ErroCampo(String campo, String mensagem) {
    }
}
