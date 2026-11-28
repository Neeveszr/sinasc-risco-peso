package br.edu.unifsa.sinasc.exception;

/**
 * O serviço de IA não pôde atender: está fora do ar, passou do tempo limite, respondeu com erro
 * ou devolveu uma resposta que não segue o contrato. Vira HTTP 503.
 */
public class ModeloIndisponivelException extends RuntimeException {

    public ModeloIndisponivelException(String mensagem) {
        super(mensagem);
    }

    public ModeloIndisponivelException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
