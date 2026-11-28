package br.edu.unifsa.sinasc.modelo;

/**
 * Um dos principais fatores da explicação (SHAP), do maior impacto para o menor.
 *
 * @param campo   nome do atributo em camelCase, o mesmo que o front usa (ex.: tipoGravidez)
 * @param rotulo  nome em português para mostrar ao usuário (ex.: "Tipo de gravidez")
 * @param valor   valor do atributo no caso analisado (texto, número ou null)
 * @param impacto magnitude do fator. A unidade ainda é provisória (depende do modelo final):
 *                use só para ordenar e NUNCA mostre como percentual
 * @param direcao se o fator aumenta ou reduz o risco
 */
public record FatorExplicacao(String campo, String rotulo, Object valor, double impacto, Direcao direcao) {
}
