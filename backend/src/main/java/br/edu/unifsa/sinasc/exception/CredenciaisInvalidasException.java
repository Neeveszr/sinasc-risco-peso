package br.edu.unifsa.sinasc.exception;

/** Vira HTTP 401. A mensagem é sempre a mesma, para não revelar se o e-mail existe. */
public class CredenciaisInvalidasException extends RuntimeException {

    public CredenciaisInvalidasException() {
        super("E-mail ou senha inválidos");
    }
}
