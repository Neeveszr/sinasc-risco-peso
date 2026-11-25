"""Estado da sessão do usuário (token e dados da conta).

O token fica em st.session_state, que vive enquanto a aba estiver aberta.
Recarregar a página (F5) encerra a sessão e pede login de novo.
"""

import streamlit as st

import api

PERFIL_OPERADOR = "OPERADOR"
PERFIL_GESTOR = "GESTOR_AUDITOR"

NOMES_PERFIL = {PERFIL_OPERADOR: "Operador", PERFIL_GESTOR: "Gestor/auditor"}


def logado():
    return "token" in st.session_state


def token():
    return st.session_state.get("token")


def usuario():
    return st.session_state.get("usuario", {})


def perfil():
    return usuario().get("perfil")


def entrar(resposta_login):
    st.session_state["token"] = resposta_login["token"]
    st.session_state["usuario"] = resposta_login["usuario"]


def sair():
    st.session_state.clear()


def mostrar_erro(erro: api.ApiErro):
    """Mostra o erro da API. Token expirado ou inválido encerra a sessão."""
    if erro.status == 401 and logado():
        sair()
        st.warning("Sua sessão expirou. Entre novamente.")
        st.rerun()
    if erro.status == 403:
        st.error("Seu perfil não tem permissão para esta operação.")
        return
    st.error(erro.mensagem)
    for detalhe in erro.erros:
        st.caption(f"• {detalhe}")
