package br.edu.unifsa.sinasc.config;

import java.util.Locale;

/**
 * Detecta valores de modelo copiados do .env.example (ex.: "troque-esta-senha").
 * Esses valores são públicos, então segredos e senhas reais nunca podem começar assim.
 */
public final class ValoresDeExemplo {

    private ValoresDeExemplo() {
    }

    public static boolean ehPlaceholder(String valor) {
        return valor != null && valor.trim().toLowerCase(Locale.ROOT).startsWith("troque");
    }
}
