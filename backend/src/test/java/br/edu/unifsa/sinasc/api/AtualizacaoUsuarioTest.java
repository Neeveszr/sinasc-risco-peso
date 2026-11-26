package br.edu.unifsa.sinasc.api;

import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import br.edu.unifsa.sinasc.usuario.Perfil;
import br.edu.unifsa.sinasc.usuario.Usuario;

/** PUT /api/usuarios/{id}: a senha é opcional (omitida = não troca); "" é rejeitada com 422. */
class AtualizacaoUsuarioTest extends ApiTestBase {

    private static final String EMAIL = "ana@teste.com";

    @Test
    void semSenhaNaoTrocaASenha() throws Exception {
        criarUsuario("gestor@teste.com", Perfil.GESTOR_AUDITOR);
        Usuario ana = criarUsuario(EMAIL, Perfil.OPERADOR);
        String token = token("gestor@teste.com", SENHA);
        String hashAntes = hashDaSenha(EMAIL);

        atualizar(ana, token, "{\"nome\":\"Ana Maria\",\"email\":\"" + EMAIL + "\",\"ativo\":true}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Ana Maria"));

        assertEquals(hashAntes, hashDaSenha(EMAIL), "o hash da senha não pode mudar");
        login(EMAIL, SENHA).andExpect(status().isOk()); // a senha antiga continua valendo
    }

    @Test
    void senhaVaziaRetorna422ENaoAlteraNada() throws Exception {
        criarUsuario("gestor@teste.com", Perfil.GESTOR_AUDITOR);
        Usuario ana = criarUsuario(EMAIL, Perfil.OPERADOR);
        String token = token("gestor@teste.com", SENHA);
        String hashAntes = hashDaSenha(EMAIL);

        atualizar(ana, token, "{\"nome\":\"Ana Maria\",\"email\":\"" + EMAIL + "\",\"senha\":\"\",\"ativo\":true}")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erros[*].campo", hasItem("senha")));

        assertEquals(hashAntes, hashDaSenha(EMAIL));
        assertEquals("Usuario Teste", nomeNoBanco(EMAIL), "nada deve ser atualizado quando a validação falha");
        login(EMAIL, SENHA).andExpect(status().isOk());
    }

    @Test
    void senhaSoComEspacosRetorna422() throws Exception {
        criarUsuario("gestor@teste.com", Perfil.GESTOR_AUDITOR);
        Usuario ana = criarUsuario(EMAIL, Perfil.OPERADOR);
        String token = token("gestor@teste.com", SENHA);

        atualizar(ana, token, "{\"nome\":\"Ana\",\"email\":\"" + EMAIL + "\",\"senha\":\"        \",\"ativo\":true}")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erros[*].campo", hasItem("senha")));

        login(EMAIL, SENHA).andExpect(status().isOk());
    }

    @Test
    void senhaNovaValidaTrocaASenha() throws Exception {
        criarUsuario("gestor@teste.com", Perfil.GESTOR_AUDITOR);
        Usuario ana = criarUsuario(EMAIL, Perfil.OPERADOR);
        String token = token("gestor@teste.com", SENHA);

        atualizar(ana, token,
                "{\"nome\":\"Ana\",\"email\":\"" + EMAIL + "\",\"senha\":\"nova-senha-123\",\"ativo\":true}")
                .andExpect(status().isOk());

        login(EMAIL, SENHA).andExpect(status().isUnauthorized());
        login(EMAIL, "nova-senha-123").andExpect(status().isOk());
    }

    // ---------- auxiliares ----------

    private ResultActions atualizar(Usuario usuario, String token, String json) throws Exception {
        return mockMvc.perform(put("/api/usuarios/" + usuario.getId())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private String hashDaSenha(String email) {
        return jdbc.queryForObject("select senha_hash from usuario where email = ?", String.class, email);
    }

    private String nomeNoBanco(String email) {
        return jdbc.queryForObject("select nome from usuario where email = ?", String.class, email);
    }
}
