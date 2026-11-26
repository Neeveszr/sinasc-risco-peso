package br.edu.unifsa.sinasc.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.edu.unifsa.sinasc.usuario.Perfil;
import br.edu.unifsa.sinasc.usuario.Usuario;
import br.edu.unifsa.sinasc.usuario.UsuarioService;

/**
 * Base dos testes de integração: sobe a API inteira (com segurança e JWT) em memória.
 * Os dados NÃO usam @Transactional de propósito: a auditoria de falhas grava em transação
 * própria, então o teste precisa de dados já confirmados (commit) no banco.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class ApiTestBase {

    protected static final String SENHA = "senha12345";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    protected UsuarioService usuarioService;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void limparBanco() {
        // log_acesso primeiro, porque referencia usuario
        jdbc.update("delete from log_acesso");
        jdbc.update("delete from usuario");
    }

    protected Usuario criarUsuario(String email, Perfil perfil) {
        return usuarioService.criar("Usuario Teste", email, SENHA, perfil);
    }

    protected ResultActions login(String email, String senha) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"senha\":\"" + senha + "\"}"));
    }

    /** Faz login (esperando sucesso) e devolve o token JWT. */
    protected String token(String email, String senha) throws Exception {
        String corpo = login(email, senha)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(corpo).get("token").asText();
    }

    /** Registros do log_acesso, lidos direto do banco (colunas: usuario_id, identificador, acao, recurso). */
    protected List<Map<String, Object>> logs() {
        return jdbc.queryForList(
                "select usuario_id, identificador, acao, recurso from log_acesso order by id");
    }
}
