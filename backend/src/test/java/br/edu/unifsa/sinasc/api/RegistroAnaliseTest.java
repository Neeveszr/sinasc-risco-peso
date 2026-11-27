
package br.edu.unifsa.sinasc.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import br.edu.unifsa.sinasc.usuario.Perfil;

class RegistroAnaliseTest extends ApiTestBase {

    private static final String REGISTRO_VALIDO = """
            {
                "faixaEtaria": "25-29",
                "estadoCivil": 5,
                "escolaridade": 3,
                "tipoGravidez": 1,
                "gestacoesAnteriores": 2,
                "partosNormais": 1,
                "partosCesareos": 0,
                "filhosVivos": 1,
                "perdasFetais": 0,
                "mesInicioPrenatal": 2
            }
            """;

    private String criarRegistro(String token) throws Exception {
        return mockMvc.perform(post("/api/registros")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(REGISTRO_VALIDO))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    @Test
    void operadorConsegueCriarEConsultarProprioRegistro() throws Exception {
        criarUsuario("ana@teste.com", Perfil.OPERADOR);
        String token = token("ana@teste.com", SENHA);

        String resposta = criarRegistro(token);

        long id = objectMapper.readTree(resposta).get("id").asLong();

        mockMvc.perform(get("/api/registros/" + id)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.faixaEtaria").value("25-29"))
                .andExpect(jsonPath("$.gestacoesAnteriores").value(2))
                .andExpect(jsonPath("$.mesInicioPrenatal").value(2));
    }

    @Test
    void operadorListaSomenteSeusRegistros() throws Exception {
        criarUsuario("ana@teste.com", Perfil.OPERADOR);
        criarUsuario("bruno@teste.com", Perfil.OPERADOR);

        String tokenAna = token("ana@teste.com", SENHA);
        String tokenBruno = token("bruno@teste.com", SENHA);

        criarRegistro(tokenAna);
        criarRegistro(tokenBruno);

        mockMvc.perform(get("/api/registros")
                .header("Authorization", "Bearer " + tokenAna))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void gestorConsegueListarRegistrosDeTodos() throws Exception {
        criarUsuario("ana@teste.com", Perfil.OPERADOR);
        criarUsuario("bruno@teste.com", Perfil.OPERADOR);
        criarUsuario("gestor@teste.com", Perfil.GESTOR_AUDITOR);

        String tokenAna = token("ana@teste.com", SENHA);
        String tokenBruno = token("bruno@teste.com", SENHA);
        String tokenGestor = token("gestor@teste.com", SENHA);

        criarRegistro(tokenAna);
        criarRegistro(tokenBruno);

        mockMvc.perform(get("/api/registros")
                .header("Authorization", "Bearer " + tokenGestor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void operadorNaoConsegueConsultarRegistroDeOutroUsuario() throws Exception {
        criarUsuario("ana@teste.com", Perfil.OPERADOR);
        criarUsuario("bruno@teste.com", Perfil.OPERADOR);

        String tokenAna = token("ana@teste.com", SENHA);
        String tokenBruno = token("bruno@teste.com", SENHA);

        String resposta = criarRegistro(tokenAna);
        long id = objectMapper.readTree(resposta).get("id").asLong();

        mockMvc.perform(get("/api/registros/" + id)
                .header("Authorization", "Bearer " + tokenBruno))
                .andExpect(status().isNotFound());
    }

    @Test
    void sistemaRejeitaSomaDePartosMaiorQueGestacoes() throws Exception {
        criarUsuario("ana@teste.com", Perfil.OPERADOR);
        String token = token("ana@teste.com", SENHA);

        String registroInvalido = REGISTRO_VALIDO
        .replace("\"gestacoesAnteriores\": 2", "\"gestacoesAnteriores\": 1")
        .replace("\"partosCesareos\": 0", "\"partosCesareos\": 1");

        mockMvc.perform(post("/api/registros")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(registroInvalido))
                .andExpect(status().isUnprocessableEntity());
    }

@Test
void gestorNaoConsegueCriarRegistro() throws Exception {
    criarUsuario("gestor@teste.com", Perfil.GESTOR_AUDITOR);
    String tokenGestor = token("gestor@teste.com", SENHA);

    mockMvc.perform(post("/api/registros")
            .header("Authorization", "Bearer " + tokenGestor)
            .contentType(MediaType.APPLICATION_JSON)
            .content(REGISTRO_VALIDO))
            .andExpect(status().isForbidden());
}

    @Test
    void requisicaoSemTokenRetorna401() throws Exception {
        mockMvc.perform(get("/api/registros"))
                .andExpect(status().isUnauthorized());
    }
}
