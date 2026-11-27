package br.edu.unifsa.sinasc.modelo;

import java.util.Locale;

/** Efeito de um fator sobre o risco (sinal da soma do SHAP). */
public enum Direcao {
    AUMENTA,
    REDUZ;

    /** @throws IllegalArgumentException se o texto não for uma direção conhecida */
    public static Direcao de(String valor) {
        if (valor == null) {
            throw new IllegalArgumentException("direção ausente");
        }
        return valueOf(valor.trim().toUpperCase(Locale.ROOT));
    }
}
