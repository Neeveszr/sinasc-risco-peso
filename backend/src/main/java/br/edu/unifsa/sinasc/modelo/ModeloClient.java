package br.edu.unifsa.sinasc.modelo;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.edu.unifsa.sinasc.analise.FaixaRisco;
import br.edu.unifsa.sinasc.exception.ModeloEntradaInvalidaException;
import br.edu.unifsa.sinasc.exception.ModeloEntradaInvalidaException.ErroCampo;
import br.edu.unifsa.sinasc.exception.ModeloIndisponivelException;

/**
 * Cliente do serviço de IA (FastAPI): POST /predict. Contrato em ml/contrato_predict.md.
 * <p>
 * Uso (por quem grava as predições):
 * <pre>
 * ResultadoModelo resultado = modeloClient.prever(dados);
 * </pre>
 * Erros:
 * <ul>
 *   <li>serviço fora do ar, timeout, erro 5xx ou resposta fora do contrato: {@link ModeloIndisponivelException} (HTTP 503);</li>
 *   <li>serviço rejeitou os dados (422): {@link ModeloEntradaInvalidaException} (HTTP 422, campos em camelCase).</li>
 * </ul>
 * A URL vem de MODEL_SERVICE_URL e o tempo limite de MODEL_SERVICE_TIMEOUT (segundos).
 */
@Component
public class ModeloClient {

    private static final Logger log = LoggerFactory.getLogger(ModeloClient.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    private final RestClient restClient;

    @Autowired
    public ModeloClient(@Value("${app.model-service.url}") String url,
                        @Value("${app.model-service.timeout-seconds}") int timeoutSegundos) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofSeconds(timeoutSegundos));
        fabrica.setReadTimeout(Duration.ofSeconds(timeoutSegundos));
        this.restClient = RestClient.builder().baseUrl(url).requestFactory(fabrica).build();
    }

    /** Usado nos testes, com um RestClient ligado a um MockRestServiceServer (sem o serviço real). */
    ModeloClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public ResultadoModelo prever(DadosPredicao dados) {
        RespostaServico resposta;
        try {
            resposta = restClient.post()
                    .uri("/predict")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(montarCorpo(dados))
                    .retrieve()
                    .body(RespostaServico.class);
        } catch (HttpClientErrorException.UnprocessableEntity e) {
            throw new ModeloEntradaInvalidaException(extrairErros(e.getResponseBodyAsString()));
        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            log.warn("Serviço de IA respondeu HTTP {}", status);
            throw new ModeloIndisponivelException("Serviço de IA respondeu com erro (HTTP " + status + ")", e);
        } catch (ResourceAccessException e) {
            // conexão recusada, DNS, tempo limite de conexão ou de leitura
            log.warn("Serviço de IA inacessível ou com timeout: {}", e.getMessage());
            throw new ModeloIndisponivelException("Serviço de IA inacessível ou demorou demais para responder", e);
        } catch (RestClientException e) {
            throw new ModeloIndisponivelException("Resposta ilegível do serviço de IA", e);
        }
        return converter(resposta);
    }

    /** Converte camelCase (front) para os nomes originais do SINASC. Envia os 10 campos, com null onde não há valor. */
    private static Map<String, Object> montarCorpo(DadosPredicao d) {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put(CampoModelo.FAIXA_ETARIA.nomeServico(), d.faixaEtaria());
        corpo.put(CampoModelo.ESCOLARIDADE.nomeServico(), d.escolaridade());
        corpo.put(CampoModelo.ESTADO_CIVIL.nomeServico(), d.estadoCivil());
        corpo.put(CampoModelo.TIPO_GRAVIDEZ.nomeServico(), d.tipoGravidez());
        corpo.put(CampoModelo.MES_INICIO_PRENATAL.nomeServico(), d.mesInicioPrenatal());
        corpo.put(CampoModelo.GESTACOES_ANTERIORES.nomeServico(), d.gestacoesAnteriores());
        corpo.put(CampoModelo.PARTOS_NORMAIS.nomeServico(), d.partosNormais());
        corpo.put(CampoModelo.PARTOS_CESAREOS.nomeServico(), d.partosCesareos());
        corpo.put(CampoModelo.FILHOS_VIVOS.nomeServico(), d.filhosVivos());
        corpo.put(CampoModelo.PERDAS_FETAIS.nomeServico(), d.perdasFetais());
        return corpo;
    }

    private static ResultadoModelo converter(RespostaServico r) {
        if (r == null) {
            throw respostaInvalida("corpo vazio");
        }
        if (r.score() == null || r.score() < 0 || r.score() > 1) {
            throw respostaInvalida("score ausente ou fora de 0 a 1");
        }
        if (r.versaoModelo() == null || r.versaoModelo().isBlank()) {
            throw respostaInvalida("versão do modelo ausente");
        }

        FaixaRisco faixa;
        try {
            faixa = faixaDe(r.faixaRisco());
        } catch (IllegalArgumentException e) {
            throw respostaInvalida("faixa de risco desconhecida");
        }

        List<FatorExplicacao> fatores = new ArrayList<>();
        if (r.fatores() != null) {
            for (FatorServico f : r.fatores()) {
                fatores.add(converterFator(f));
            }
        }
        return new ResultadoModelo(r.score(), faixa, List.copyOf(fatores), r.versaoModelo());
    }

    private static FatorExplicacao converterFator(FatorServico f) {
        if (f == null || f.atributo() == null || f.atributo().isBlank() || f.impacto() == null) {
            throw respostaInvalida("fator da explicação incompleto");
        }
        Direcao direcao;
        try {
            direcao = Direcao.de(f.direcao());
        } catch (IllegalArgumentException e) {
            throw respostaInvalida("direção de fator desconhecida");
        }
        Optional<CampoModelo> campo = CampoModelo.porNomeServico(f.atributo());
        String nomeFront = campo.map(CampoModelo::nomeFront).orElse(f.atributo());
        String rotulo = campo.map(CampoModelo::rotulo).orElse(f.atributo());
        return new FatorExplicacao(nomeFront, rotulo, f.valor(), f.impacto(), direcao);
    }

    /** Lê o erro 422 do FastAPI ({"detail":[{"loc":["body","CAMPO"],"msg":"..."}]}) e traduz os nomes dos campos. */
    private static List<ErroCampo> extrairErros(String corpo) {
        List<ErroCampo> erros = new ArrayList<>();
        try {
            JsonNode detalhes = JSON.readTree(corpo).path("detail");
            if (detalhes.isArray()) {
                for (JsonNode item : detalhes) {
                    JsonNode loc = item.path("loc");
                    String nome = loc.isArray() && loc.size() > 0 ? loc.get(loc.size() - 1).asText() : "";
                    String campo = CampoModelo.porNomeServico(nome).map(CampoModelo::nomeFront).orElse(nome);
                    String mensagem = item.path("msg").asText("valor rejeitado");
                    erros.add(new ErroCampo(campo, "rejeitado pelo modelo: " + mensagem));
                }
            }
        } catch (JsonProcessingException | RuntimeException e) {
            // corpo fora do padrão: segue sem detalhes por campo
        }
        return erros;
    }

    /** Texto do contrato (BAIXO, MEDIO ou ALTO, sem acento) para o enum único FaixaRisco, do pacote analise. */
    private static FaixaRisco faixaDe(String valor) {
        if (valor == null) {
            throw new IllegalArgumentException("faixa de risco ausente");
        }
        return FaixaRisco.valueOf(valor.trim().toUpperCase(Locale.ROOT));
    }

    private static ModeloIndisponivelException respostaInvalida(String motivo) {
        return new ModeloIndisponivelException("Resposta inválida do serviço de IA: " + motivo);
    }

    // formato da resposta do serviço (nomes em snake_case, como no contrato)

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RespostaServico(
            // o serviço passou a devolver "score"; "probabilidade" ainda é aceito até o contrato ser atualizado
            @JsonAlias("probabilidade") Double score,
            @JsonProperty("faixa_risco") String faixaRisco,
            List<FatorServico> fatores,
            @JsonProperty("versao_modelo") String versaoModelo) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record FatorServico(String atributo, Object valor, Double impacto, String direcao) {
    }
}
