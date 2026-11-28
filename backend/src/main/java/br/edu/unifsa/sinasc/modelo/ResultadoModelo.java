package br.edu.unifsa.sinasc.modelo;

import java.util.List;

import br.edu.unifsa.sinasc.analise.FaixaRisco;

/**
 * Resposta do serviço de IA, já validada e convertida.
 *
 * @param score        pontuação do modelo para baixo peso ao nascer (de 0 a 1). O modelo não é calibrado:
 *                     NÃO apresente como probabilidade nem como "chance"
 * @param faixaRisco   BAIXO, MEDIO ou ALTO
 * @param fatores      os 3 principais fatores, do maior impacto para o menor
 * @param versaoModelo versão do modelo usado (vai para o histórico de predições)
 */
public record ResultadoModelo(
        double score,
        FaixaRisco faixaRisco,
        List<FatorExplicacao> fatores,
        String versaoModelo) {
}
