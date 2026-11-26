package br.edu.unifsa.sinasc.api;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import br.edu.unifsa.sinasc.security.AuthUser;
import br.edu.unifsa.sinasc.usuario.Perfil;
import br.edu.unifsa.sinasc.usuario.Usuario;

/** Falhas de login (401 + LOGIN_FALHA com o motivo certo) e filtros de GET /api/auditoria/acessos. */
class LoginAuditoriaTest extends ApiTestBase {

    // ---------- falhas de login ----------

    @Test
    void emailInexistenteRetorna401EGravaLoginFalha() throws Exception {
        login("ninguem@teste.com", SENHA).andExpect(status().isUnauthorized());

        Map<String, Object> registro = umUnicoRegistro();
        assertEquals("LOGIN_FALHA", registro.get("acao"));
        assertEquals("auth/email-desconhecido", registro.get("recurso"));
        assertEquals("ninguem@teste.com", registro.get("identificador"));
        assertNull(registro.get("usuario_id"));
    }

    @Test
    void senhaErradaRetorna401EGravaLoginFalhaVinculadoAoUsuario() throws Exception {
        Usuario ana = criarUsuario("ana@teste.com", Perfil.OPERADOR);

        login("ana@teste.com", "senha-errada-123").andExpect(status().isUnauthorized());

        Map<String, Object> registro = umUnicoRegistro();
        assertEquals("LOGIN_FALHA", registro.get("acao"));
        assertEquals("auth/senha-incorreta", registro.get("recurso"));
        assertEquals("ana@teste.com", registro.get("identificador"));
        assertEquals(ana.getId().longValue(), ((Number) registro.get("usuario_id")).longValue());
    }

    @Test
    void contaInativaRetorna401EGravaLoginFalha() throws Exception {
        Usuario ana = criarUsuario("ana@teste.com", Perfil.OPERADOR);
        Usuario gestor = criarUsuario("gestor@teste.com", Perfil.GESTOR_AUDITOR);
        usuarioService.desativar(ana.getId(), comoUsuarioLogado(gestor));

        // a senha está certa, mas a conta está inativa
        login("ana@teste.com", SENHA).andExpect(status().isUnauthorized());

        Map<String, Object> registro = umUnicoRegistro();
        assertEquals("LOGIN_FALHA", registro.get("acao"));
        assertEquals("auth/conta-inativa", registro.get("recurso"));
        assertEquals(ana.getId().longValue(), ((Number) registro.get("usuario_id")).longValue());
    }

    @Test
    void asTresFalhasDevolvemExatamenteAMesmaRespostaParaQuemTentaLogar() throws Exception {
        Usuario ana = criarUsuario("ana@teste.com", Perfil.OPERADOR);
        Usuario inativa = criarUsuario("inativa@teste.com", Perfil.OPERADOR);
        usuarioService.desativar(inativa.getId(), comoUsuarioLogado(ana));

        String emailInexistente = corpo401(login("ninguem@teste.com", SENHA));
        String senhaErrada = corpo401(login("ana@teste.com", "senha-errada-123"));
        String contaInativa = corpo401(login("inativa@teste.com", SENHA));

        // o motivo real só aparece na auditoria, nunca para quem tenta logar
        assertEquals(emailInexistente, senhaErrada);
        assertEquals(emailInexistente, contaInativa);
    }

    // ---------- GET /api/auditoria/acessos ----------

    @Test
    void filtroPorAcaoDevolveSoAsFalhasDeLogin() throws Exception {
        criarUsuario("gestor@teste.com", Perfil.GESTOR_AUDITOR);
        criarUsuario("ana@teste.com", Perfil.OPERADOR);
        String token = token("gestor@teste.com", SENHA); // gera 1 registro LOGIN (sucesso)
        login("ninguem@teste.com", SENHA).andExpect(status().isUnauthorized());
        login("ana@teste.com", "senha-errada-123").andExpect(status().isUnauthorized());

        // sem filtro: 1 sucesso + 2 falhas
        mockMvc.perform(get("/api/auditoria/acessos").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3));

        // com acao=LOGIN_FALHA: só as 2 falhas
        mockMvc.perform(get("/api/auditoria/acessos")
                        .param("acao", "LOGIN_FALHA")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[*].acao", everyItem(is("LOGIN_FALHA"))))
                .andExpect(jsonPath("$.content[*].identificador", hasItem("ninguem@teste.com")));
    }

    @Test
    void filtroPorUsuarioEAcaoDevolveSoAsFalhasDaquelaConta() throws Exception {
        criarUsuario("gestor@teste.com", Perfil.GESTOR_AUDITOR);
        Usuario ana = criarUsuario("ana@teste.com", Perfil.OPERADOR);
        String token = token("gestor@teste.com", SENHA);
        login("ninguem@teste.com", SENHA).andExpect(status().isUnauthorized());
        login("ana@teste.com", "senha-errada-123").andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/auditoria/acessos")
                        .param("acao", "LOGIN_FALHA")
                        .param("usuarioId", String.valueOf(ana.getId()))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].usuarioEmail").value("ana@teste.com"))
                .andExpect(jsonPath("$.content[0].recurso").value("auth/senha-incorreta"));
    }

    @Test
    void operadorNaoAcessaAAuditoria() throws Exception {
        criarUsuario("ana@teste.com", Perfil.OPERADOR);
        String token = token("ana@teste.com", SENHA);

        mockMvc.perform(get("/api/auditoria/acessos").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void semTokenARotaDeAuditoriaRetorna401() throws Exception {
        mockMvc.perform(get("/api/auditoria/acessos")).andExpect(status().isUnauthorized());
    }

    // ---------- auxiliares ----------

    private Map<String, Object> umUnicoRegistro() {
        List<Map<String, Object>> registros = logs();
        assertEquals(1, registros.size(), "deveria haver exatamente 1 registro de auditoria");
        return registros.get(0);
    }

    private static AuthUser comoUsuarioLogado(Usuario usuario) {
        return new AuthUser(usuario.getId(), usuario.getEmail(), usuario.getPerfil());
    }

    private static String corpo401(ResultActions resposta) throws Exception {
        return resposta.andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
    }
}
