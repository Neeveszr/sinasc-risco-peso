package br.edu.unifsa.sinasc.exception;

/** Vira HTTP 409 (ex.: e-mail já cadastrado, ação não permitida no estado atual). */
public class ConflitoException extends RuntimeException {

    public ConflitoException(String mensagem) {
        super(mensagem);
    }
}
