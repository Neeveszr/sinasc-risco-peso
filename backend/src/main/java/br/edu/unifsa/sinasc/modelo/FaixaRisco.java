package br.edu.unifsa.sinasc.modelo;

import java.util.Locale;

/** Faixa de risco devolvida pelo serviço de IA (sem acento, como no contrato). */
public enum FaixaRisco {
    BAIXO,
    MEDIO,
    ALTO;

    /** @throws IllegalArgumentException se o texto não for uma faixa conhecida */
    public static FaixaRisco de(String valor) {
        if (valor == null) {
            throw new IllegalArgumentException("faixa de risco ausente");
        }
        return valueOf(valor.trim().toUpperCase(Locale.ROOT));
    }
}
