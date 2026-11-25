import streamlit as st

import api
import config
import sessao

st.title(config.NOME_SISTEMA)
st.write("Apoio à equipe de saúde da família na priorização do acompanhamento pré-natal.")

with st.form("login"):
    email = st.text_input("E-mail")
    senha = st.text_input("Senha", type="password")
    entrar = st.form_submit_button("Entrar", type="primary")

if config.MODO_DEMO:
    st.caption("Demonstração: qualquer senha funciona. E-mail começando com \"gestor\" entra como gestor/auditor; os demais, como operador.")

if entrar:
    if not email or not senha:
        st.error("Informe e-mail e senha.")
    else:
        try:
            sessao.entrar(api.login(email.strip(), senha))
            st.rerun()
        except api.ApiErro as erro:
            # Mensagem genérica de propósito: não revela se o e-mail existe.
            st.error("E-mail ou senha inválidos." if erro.status == 401 else erro.mensagem)
