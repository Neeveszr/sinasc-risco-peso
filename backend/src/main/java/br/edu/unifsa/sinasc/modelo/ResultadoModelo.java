package br.edu.unifsa.sinasc.modelo;

import java.util.List;

/**
 * Resposta do serviço de IA, já validada e convertida.
 *
 * @param probabilidade probabilidade de baixo peso ao nascer (de 0 a 1)
 * @param faixaRisco    BAIXO, MEDIO ou ALTO
 * @param fatores       os 3 principais fatores, do maior impacto para o menor
 * @param versaoModelo  versão do modelo usado (vai para o histórico de predições)
 */
public record ResultadoModelo(
        double probabilidade,
        FaixaRisco faixaRisco,
        List<FatorExplicacao> fatores,
        String versaoModelo) {
}
