package br.edu.unifsa.sinasc.modelo;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.ConnectException;
import java.net.SocketTimeoutException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import br.edu.unifsa.sinasc.exception.GlobalExceptionHandler;

/**
 * Como os erros do serviço de IA chegam a quem chama a API: 503 e 422 no formato padrão (problem+json).
 * Usa um controller de teste mínimo, só para exercitar o ModeloClient e o GlobalExceptionHandler.
 */
class ModeloErrosHttpTest {

    private static final String URL_PREDICT = "http://modelo.teste/predict";

    private static final DadosPredicao DADOS =
            new DadosPredicao("25-29", 3, 5, 1, 2, 1, 1, 0, 1, 0);

    @RestController
    static class ControladorDeTeste {

        private final ModeloClient cliente;

        ControladorDeTeste(ModeloClient cliente) {
            this.cliente = cliente;
        }

        @PostMapping("/teste/prever")
        public ResultadoModelo prever() {
            return cliente.prever(DADOS);
        }
    }

    private MockRestServiceServer servidor;
    private MockMvc mockMvc;

    @BeforeEach
    void preparar() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://modelo.teste");
        servidor = MockRestServiceServer.bindTo(builder).build();
        ModeloClient cliente = new ModeloClient(builder.build());
        mockMvc = MockMvcBuilders.standaloneSetup(new ControladorDeTeste(cliente))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void respostaNormalDevolve200() throws Exception {
        servidor.expect(requestTo(URL_PREDICT)).andRespond(withSuccess("""
                {"score": 0.62, "faixa_risco": "ALTO", "versao_modelo": "0.0.0-mock",
                 "fatores": [{"atributo": "GRAVIDEZ", "valor": 2, "impacto": 0.41, "direcao": "AUMENTA"}]}
                """, MediaType.APPLICATION_JSON));

        mockMvc.perform(post("/teste/prever"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(0.62))
                .andExpect(jsonPath("$.faixaRisco").value("ALTO"))
                .andExpect(jsonPath("$.versaoModelo").value("0.0.0-mock"))
                .andExpect(jsonPath("$.fatores[0].campo").value("tipoGravidez"))
                .andExpect(jsonPath("$.fatores[0].direcao").value("AUMENTA"));
    }

    @Test
    void servicoForaDoArDevolve503NoFormatoPadrao() throws Exception {
        servidor.expect(requestTo(URL_PREDICT)).andRespond(requisicao -> {
            throw new ConnectException("Connection refused");
        });

        mockMvc.perform(post("/teste/prever"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.title").exists())
                .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    void timeoutDevolve503() throws Exception {
        servidor.expect(requestTo(URL_PREDICT)).andRespond(requisicao -> {
            throw new SocketTimeoutException("Read timed out");
        });

        mockMvc.perform(post("/teste/prever"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503));
    }

    @Test
    void erro500DoServicoDevolve503() throws Exception {
        servidor.expect(requestTo(URL_PREDICT)).andRespond(withServerError());

        mockMvc.perform(post("/teste/prever"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void erro422DoServicoDevolve422NoFormatoPadraoComCampoEmCamelCase() throws Exception {
        servidor.expect(requestTo(URL_PREDICT))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {"detail": [{"type": "less_than_equal", "loc": ["body", "MESPRENAT"],
                                 "msg": "Input should be less than or equal to 9"}]}
                                """));

        mockMvc.perform(post("/teste/prever"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.erros[0].campo").value("mesInicioPrenatal"));
    }
}
