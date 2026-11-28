package br.edu.unifsa.sinasc.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

/** Regras do contrato (ml/contrato_predict.md) que a API também valida na entrada. */
class DadosPredicaoValidacaoTest {

    private static final Validator VALIDADOR = Validation.buildDefaultValidatorFactory().getValidator();

    /** Exemplo do contrato. */
    private static DadosPredicao valido() {
        return new DadosPredicao("25-29", 3, 5, 1, 2, 1, 1, 0, 1, 0);
    }

    private static Set<String> camposInvalidos(DadosPredicao dados) {
        return VALIDADOR.validate(dados).stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }

    @Test
    void exemploDoContratoEValido() {
        assertEquals(Set.of(), camposInvalidos(valido()));
    }

    @Test
    void camposOpcionaisAceitamNulo() {
        DadosPredicao dados = new DadosPredicao(null, null, null, null, null, 0, 0, 0, 0, 0);

        assertEquals(Set.of(), camposInvalidos(dados));
    }

    @Test
    void codigoNoveEValidoEmEscolaridadeEstadoCivilETipoDeGravidez() {
        DadosPredicao dados = new DadosPredicao(">=50", 9, 9, 9, 9, 15, 7, 8, 15, 15);

        assertEquals(Set.of(), camposInvalidos(dados));
    }

    @Test
    void escolaridadeEntreSeisEOitoEInvalida() {
        assertTrue(camposInvalidos(new DadosPredicao("25-29", 6, 5, 1, 2, 1, 1, 0, 1, 0)).contains("escolaridade"));
        assertTrue(camposInvalidos(new DadosPredicao("25-29", 8, 5, 1, 2, 1, 1, 0, 1, 0)).contains("escolaridade"));
    }

    @Test
    void estadoCivilZeroEInvalido() {
        assertTrue(camposInvalidos(new DadosPredicao("25-29", 3, 0, 1, 2, 1, 1, 0, 1, 0)).contains("estadoCivil"));
    }

    @Test
    void tipoDeGravidezQuatroEInvalido() {
        assertTrue(camposInvalidos(new DadosPredicao("25-29", 3, 5, 4, 2, 1, 1, 0, 1, 0)).contains("tipoGravidez"));
    }

    @Test
    void faixaEtariaExigeHifenENaoAceitaTravessaoNemIgn() {
        assertTrue(camposInvalidos(new DadosPredicao("40\u201344", 3, 5, 1, 2, 1, 1, 0, 1, 0)).contains("faixaEtaria"));
        assertTrue(camposInvalidos(new DadosPredicao("Ign", 3, 5, 1, 2, 1, 1, 0, 1, 0)).contains("faixaEtaria"));
        assertTrue(camposInvalidos(new DadosPredicao("20-30", 3, 5, 1, 2, 1, 1, 0, 1, 0)).contains("faixaEtaria"));
    }

    @Test
    void mesDeInicioDoPrenatalPrecisaEstarEntreUmENove() {
        assertTrue(camposInvalidos(new DadosPredicao("25-29", 3, 5, 1, 0, 1, 1, 0, 1, 0)).contains("mesInicioPrenatal"));
        assertTrue(camposInvalidos(new DadosPredicao("25-29", 3, 5, 1, 10, 1, 1, 0, 1, 0)).contains("mesInicioPrenatal"));
    }

    @Test
    void contagensSaoObrigatoriasDeZeroAQuinze() {
        assertTrue(camposInvalidos(new DadosPredicao("25-29", 3, 5, 1, 2, null, 1, 0, 1, 0)).contains("gestacoesAnteriores"));
        assertTrue(camposInvalidos(new DadosPredicao("25-29", 3, 5, 1, 2, 1, null, 0, 1, 0)).contains("partosNormais"));
        assertTrue(camposInvalidos(new DadosPredicao("25-29", 3, 5, 1, 2, 1, 1, null, 1, 0)).contains("partosCesareos"));
        assertTrue(camposInvalidos(new DadosPredicao("25-29", 3, 5, 1, 2, 1, 1, 0, null, 0)).contains("filhosVivos"));
        assertTrue(camposInvalidos(new DadosPredicao("25-29", 3, 5, 1, 2, 1, 1, 0, 1, null)).contains("perdasFetais"));

        assertTrue(camposInvalidos(new DadosPredicao("25-29", 3, 5, 1, 2, 16, 1, 0, 1, 0)).contains("gestacoesAnteriores"));
        assertTrue(camposInvalidos(new DadosPredicao("25-29", 3, 5, 1, 2, 1, 1, 0, 1, -1)).contains("perdasFetais"));
    }

    @Test
    void partosNormaisMaisCesareosNaoPodemPassarDasGestacoesAnteriores() {
        // 1 + 1 > 1
        assertTrue(camposInvalidos(new DadosPredicao("25-29", 3, 5, 1, 2, 1, 1, 1, 1, 0))
                .contains("contagemConsistente"));
        // 1 + 1 <= 2 (limite exato é válido)
        assertEquals(Set.of(), camposInvalidos(new DadosPredicao("25-29", 3, 5, 1, 2, 2, 1, 1, 1, 0)));
    }
}
