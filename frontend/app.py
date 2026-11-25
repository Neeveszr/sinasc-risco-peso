"""Ponto de entrada do front-end (Streamlit).

Executar a partir da pasta frontend/:  streamlit run app.py
O menu depende do perfil. Esconder telas é só conveniência: quem garante
o acesso é o back-end, que responde 403 a quem não tem permissão.
"""

import streamlit as st

import config
import sessao

st.set_page_config(page_title=config.NOME_SISTEMA, page_icon=":material/monitor_heart:", layout="centered")

if config.MODO_DEMO:
    st.warning("**Modo demonstração:** os resultados são fictícios e não vêm do modelo.", icon=":material/science:")

if not sessao.logado():
    st.navigation([st.Page("paginas/login.py", title="Entrar", icon=":material/login:")], position="hidden").run()
    st.stop()

predicao = st.Page("paginas/predicao.py", title="Nova predição", icon=":material/edit_note:")
resultado = st.Page("paginas/resultado.py", title="Resultado", icon=":material/assignment:")
auditoria = st.Page("paginas/auditoria.py", title="Auditoria", icon=":material/fact_check:")
transparencia = st.Page("paginas/transparencia.py", title="Sobre o modelo", icon=":material/info:")
privacidade = st.Page("paginas/privacidade.py", title="Privacidade", icon=":material/shield_person:")

if sessao.perfil() == sessao.PERFIL_GESTOR:
    paginas = {"Gestão": [auditoria], "Informações": [transparencia, privacidade]}
else:
    paginas = {"Atendimento": [predicao, resultado], "Informações": [transparencia, privacidade]}

with st.sidebar:
    st.markdown(f"**{sessao.usuario().get('nome', '')}**")
    st.caption(sessao.NOMES_PERFIL.get(sessao.perfil(), sessao.perfil()))
    if st.button("Sair", icon=":material/logout:"):
        sessao.sair()
        st.rerun()

st.navigation(paginas).run()
