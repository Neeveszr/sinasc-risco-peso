package br.edu.unifsa.sinasc.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.edu.unifsa.sinasc.analise.FaixaRisco;
import br.edu.unifsa.sinasc.exception.ModeloEntradaInvalidaException;
import br.edu.unifsa.sinasc.exception.ModeloIndisponivelException;

/** O cliente do serviço de IA, sem o serviço real (MockRestServiceServer). */
class ModeloClientTest {

    private static final String URL_PREDICT = "http://modelo.teste/predict";

    /** Exemplo do contrato, com escolaridade e mês do pré-natal nulos. */
    private static final DadosPredicao DADOS =
            new DadosPredicao("25-29", null, 5, 1, null, 1, 1, 0, 1, 0);

    private static final String RESPOSTA_NORMAL = """
            {
              "score": 0.62,
              "faixa_risco": "ALTO",
              "fatores": [
                {"atributo": "GRAVIDEZ", "valor": 2, "impacto": 0.41, "direcao": "AUMENTA"},
                {"atributo": "MESPRENAT", "valor": null, "impacto": 0.12, "direcao": "AUMENTA"},
                {"atributo": "FxEtPartur", "valor": "25-29", "impacto": 0.05, "direcao": "REDUZ"}
              ],
              "versao_modelo": "0.0.0-mock"
            }
            """;

    private MockRestServiceServer servidor;
    private ModeloClient cliente;

    @BeforeEach
    void preparar() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://modelo.teste");
        servidor = MockRestServiceServer.bindTo(builder).build();
        cliente = new ModeloClient(builder.build());
    }

    // ---------- resposta normal ----------

    @Test
    void respostaNormalViraResultadoComFatoresTraduzidos() {
        servidor.expect(requestTo(URL_PREDICT))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(RESPOSTA_NORMAL, MediaType.APPLICATION_JSON));

        ResultadoModelo resultado = cliente.prever(DADOS);

        assertEquals(0.62, resultado.score(), 0.0001);
        assertEquals(FaixaRisco.ALTO, resultado.faixaRisco());
        assertEquals("0.0.0-mock", resultado.versaoModelo());
        assertEquals(3, resultado.fatores().size());

        FatorExplicacao primeiro = resultado.fatores().get(0);
        assertEquals("tipoGravidez", primeiro.campo());        // nome do SINASC virou camelCase
        assertEquals("Tipo de gravidez", primeiro.rotulo());
        assertEquals(2, ((Number) primeiro.valor()).intValue());
        assertEquals(0.41, primeiro.impacto(), 0.0001);
        assertEquals(Direcao.AUMENTA, primeiro.direcao());

        FatorExplicacao segundo = resultado.fatores().get(1);
        assertEquals("mesInicioPrenatal", segundo.campo());
        assertNull(segundo.valor());

        FatorExplicacao terceiro = resultado.fatores().get(2);
        assertEquals("faixaEtaria", terceiro.campo());
        assertEquals("25-29", terceiro.valor());
        assertEquals(Direcao.REDUZ, terceiro.direcao());

        servidor.verify();
    }

    @Test
    void enviaOsDezCamposComOsNomesDoSinascEComNulosExplicitos() {
        servidor.expect(requestTo(URL_PREDICT))
                .andExpect(requisicao -> {
                    String corpo = ((MockClientHttpRequest) requisicao).getBodyAsString();
                    JsonNode json = new ObjectMapper().readTree(corpo);

                    assertEquals(10, json.size(), "o serviço só aceita os 10 atributos do contrato");
                    assertEquals("25-29", json.get("FxEtPartur").asText());
                    assertTrue(json.get("ESCMAE2010").isNull());
                    assertEquals(5, json.get("ESTCIVMAE").asInt());
                    assertEquals(1, json.get("GRAVIDEZ").asInt());
                    assertTrue(json.get("MESPRENAT").isNull());
                    assertEquals(1, json.get("QTDGESTANT").asInt());
                    assertEquals(1, json.get("QTDPARTNOR").asInt());
                    assertEquals(0, json.get("QTDPARTCES").asInt());
                    assertEquals(1, json.get("QTDFILVIVO").asInt());
                    assertEquals(0, json.get("QTDFILMORT").asInt());
                })
                .andRespond(withSuccess(RESPOSTA_NORMAL, MediaType.APPLICATION_JSON));

        cliente.prever(DADOS);

        servidor.verify();
    }

    // ---------- serviço fora do ar ----------

    @Test
    void conexaoRecusadaViraModeloIndisponivel() {
        servidor.expect(requestTo(URL_PREDICT))
                .andRespond(requisicao -> {
                    throw new ConnectException("Connection refused");
                });

        assertThrows(ModeloIndisponivelException.class, () -> cliente.prever(DADOS));
    }

    @Test
    void timeoutViraModeloIndisponivel() {
        servidor.expect(requestTo(URL_PREDICT))
                .andRespond(requisicao -> {
                    throw new SocketTimeoutException("Read timed out");
                });

        assertThrows(ModeloIndisponivelException.class, () -> cliente.prever(DADOS));
    }

    @Test
    void erro500DoServicoViraModeloIndisponivel() {
        servidor.expect(requestTo(URL_PREDICT)).andRespond(withServerError());

        assertThrows(ModeloIndisponivelException.class, () -> cliente.prever(DADOS));
    }

    // ---------- erro 422 do serviço ----------

    @Test
    void erro422DoServicoViraEntradaInvalidaComCampoEmCamelCase() {
        servidor.expect(requestTo(URL_PREDICT))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {"detail": [{"type": "less_than_equal", "loc": ["body", "MESPRENAT"],
                                 "msg": "Input should be less than or equal to 9"}]}
                                """));

        ModeloEntradaInvalidaException erro =
                assertThrows(ModeloEntradaInvalidaException.class, () -> cliente.prever(DADOS));

        assertEquals(1, erro.getErros().size());
        assertEquals("mesInicioPrenatal", erro.getErros().get(0).campo());
        assertTrue(erro.getErros().get(0).mensagem().contains("less than or equal to 9"));
    }

    @Test
    void erro422ComCorpoForaDoPadraoNaoQuebra() {
        servidor.expect(requestTo(URL_PREDICT))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY)
                        .contentType(MediaType.TEXT_PLAIN)
                        .body("isso não é JSON"));

        ModeloEntradaInvalidaException erro =
                assertThrows(ModeloEntradaInvalidaException.class, () -> cliente.prever(DADOS));

        assertEquals(List.of(), erro.getErros());
    }

    // ---------- resposta fora do contrato ----------

    @Test
    void ausenciaDeScoreViraModeloIndisponivel() {
        responderCom("""
                {"faixa_risco": "MEDIO", "fatores": [], "versao_modelo": "1.0"}
                """);

        assertThrows(ModeloIndisponivelException.class, () -> cliente.prever(DADOS));
    }

    /** Enquanto o contrato (e o serviço) não trocam para "score", o nome antigo continua funcionando. */
    @Test
    void aindaAceitaONomeAntigoProbabilidadeComoScore() {
        responderCom("""
                {"probabilidade": 0.31, "faixa_risco": "ALTO", "fatores": [], "versao_modelo": "1.0"}
                """);

        ResultadoModelo resultado = cliente.prever(DADOS);

        assertEquals(0.31, resultado.score(), 0.0001);
        assertEquals(FaixaRisco.ALTO, resultado.faixaRisco());
    }

    @Test
    void faixaDeRiscoDesconhecidaViraModeloIndisponivel() {
        responderCom("""
                {"score": 0.5, "faixa_risco": "CRITICO", "fatores": [], "versao_modelo": "1.0"}
                """);

        assertThrows(ModeloIndisponivelException.class, () -> cliente.prever(DADOS));
    }

    @Test
    void scoreForaDeZeroAUmViraModeloIndisponivel() {
        responderCom("""
                {"score": 1.5, "faixa_risco": "ALTO", "fatores": [], "versao_modelo": "1.0"}
                """);

        assertThrows(ModeloIndisponivelException.class, () -> cliente.prever(DADOS));
    }

    @Test
    void semVersaoDoModeloViraModeloIndisponivel() {
        responderCom("""
                {"score": 0.5, "faixa_risco": "MEDIO", "fatores": []}
                """);

        assertThrows(ModeloIndisponivelException.class, () -> cliente.prever(DADOS));
    }

    @Test
    void direcaoDeFatorDesconhecidaViraModeloIndisponivel() {
        responderCom("""
                {"score": 0.5, "faixa_risco": "MEDIO", "versao_modelo": "1.0",
                 "fatores": [{"atributo": "GRAVIDEZ", "valor": 2, "impacto": 0.4, "direcao": "TALVEZ"}]}
                """);

        assertThrows(ModeloIndisponivelException.class, () -> cliente.prever(DADOS));
    }

    @Test
    void corpoVazioViraModeloIndisponivel() {
        servidor.expect(requestTo(URL_PREDICT)).andRespond(withSuccess());

        assertThrows(ModeloIndisponivelException.class, () -> cliente.prever(DADOS));
    }

    @Test
    void campoExtraNaRespostaEIgnoradoEFatoresAusentesViramListaVazia() {
        responderCom("""
                {"score": 0.08, "faixa_risco": "BAIXO", "versao_modelo": "1.0", "campo_novo": "x"}
                """);

        ResultadoModelo resultado = cliente.prever(DADOS);

        assertEquals(FaixaRisco.BAIXO, resultado.faixaRisco());
        assertEquals(List.of(), resultado.fatores());
    }

    private void responderCom(String json) {
        servidor.expect(requestTo(URL_PREDICT))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));
    }
}
