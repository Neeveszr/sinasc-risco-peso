package br.edu.unifsa.sinasc.modelo;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Dados do pré-natal que o back-end envia ao serviço de IA, com os nomes em camelCase que o front usa
 * (governanca/saida/06_contrato_entrada_api.json). O {@link ModeloClient} converte para os nomes do SINASC.
 * <p>
 * As regras de validação seguem o contrato (ml/contrato_predict.md). Quem receber esses dados de um
 * cliente HTTP pode validá-los com {@code @Valid}. Campos opcionais aceitam null
 * (null = ignorado; em mesInicioPrenatal, null = pré-natal não iniciado ou não informado).
 */
public record DadosPredicao(

        @Pattern(regexp = "10-14|15-19|20-24|25-29|30-34|35-39|40-44|45-49|>=50",
                message = "faixa etária inválida (use 10-14, 15-19, 20-24, 25-29, 30-34, 35-39, 40-44, 45-49 ou >=50, com hífen)")
        String faixaEtaria,

        @ValorEm(valores = {0, 1, 2, 3, 4, 5, 9}, message = "escolaridade deve ser de 0 a 5 ou 9 (ignorado)")
        Integer escolaridade,

        @ValorEm(valores = {1, 2, 3, 4, 5, 9}, message = "estado civil deve ser de 1 a 5 ou 9 (ignorado)")
        Integer estadoCivil,

        @ValorEm(valores = {1, 2, 3, 9}, message = "tipo de gravidez deve ser 1, 2, 3 ou 9 (ignorado)")
        Integer tipoGravidez,

        @Min(value = 1, message = "mês de início do pré-natal deve ser de 1 a 9")
        @Max(value = 9, message = "mês de início do pré-natal deve ser de 1 a 9")
        Integer mesInicioPrenatal,

        @NotNull(message = "gestações anteriores é obrigatório")
        @Min(value = 0, message = "gestações anteriores deve ser de 0 a 15")
        @Max(value = 15, message = "gestações anteriores deve ser de 0 a 15")
        Integer gestacoesAnteriores,

        @NotNull(message = "partos normais é obrigatório")
        @Min(value = 0, message = "partos normais deve ser de 0 a 15")
        @Max(value = 15, message = "partos normais deve ser de 0 a 15")
        Integer partosNormais,

        @NotNull(message = "partos cesáreos é obrigatório")
        @Min(value = 0, message = "partos cesáreos deve ser de 0 a 15")
        @Max(value = 15, message = "partos cesáreos deve ser de 0 a 15")
        Integer partosCesareos,

        @NotNull(message = "filhos vivos é obrigatório")
        @Min(value = 0, message = "filhos vivos deve ser de 0 a 15")
        @Max(value = 15, message = "filhos vivos deve ser de 0 a 15")
        Integer filhosVivos,

        @NotNull(message = "perdas fetais é obrigatório")
        @Min(value = 0, message = "perdas fetais deve ser de 0 a 15")
        @Max(value = 15, message = "perdas fetais deve ser de 0 a 15")
        Integer perdasFetais) {

    /** Regra entre campos do contrato: partos normais + partos cesáreos não pode passar das gestações anteriores. */
    @JsonIgnore
    @AssertTrue(message = "partos normais + partos cesáreos não pode ser maior que gestações anteriores")
    public boolean isContagemConsistente() {
        if (gestacoesAnteriores == null || partosNormais == null || partosCesareos == null) {
            return true; // a ausência já é apontada pelo @NotNull de cada campo
        }
        return partosNormais + partosCesareos <= gestacoesAnteriores;
    }
}
