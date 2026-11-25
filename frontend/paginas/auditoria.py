from datetime import date, timedelta

import pandas as pd
import streamlit as st

import api
import sessao

FAIXAS = {None: "Todas", "BAIXO": "Baixo", "MEDIO": "Médio", "ALTO": "Alto"}

st.title("Auditoria das predições")

if sessao.perfil() != sessao.PERFIL_GESTOR:
    st.error("Esta tela é exclusiva do perfil gestor/auditor.")
    st.stop()


@st.cache_data(ttl=300, show_spinner=False)
def carregar_usuarios(token):
    """Lista de usuários para o filtro, guardada por 5 minutos."""
    return api.listar_usuarios(token)


try:
    usuarios = carregar_usuarios(sessao.token())
except api.ApiErro as erro:
    sessao.mostrar_erro(erro)
    usuarios = []
nomes = {u["id"]: u["nome"] + ("" if u.get("ativo", True) else " (inativo)") for u in usuarios}

hoje = date.today()
with st.form("filtros"):
    c1, c2, c3 = st.columns([2, 1.5, 1])
    periodo = c1.date_input("Período", value=(hoje - timedelta(days=30), hoje), max_value=hoje, format="DD/MM/YYYY")
    usuario_id = c2.selectbox("Usuário", [None, *nomes], format_func=lambda i: "Todos" if i is None else nomes[i])
    faixa = c3.selectbox("Faixa de risco", list(FAIXAS), format_func=FAIXAS.get)
    st.form_submit_button("Filtrar", icon=":material/filter_alt:")

# Enquanto o usuário escolhe o intervalo, o seletor pode devolver só a data inicial.
inicio, fim = (periodo[0], periodo[-1]) if isinstance(periodo, (tuple, list)) and periodo else (None, None)

try:
    pagina = api.listar_predicoes(sessao.token(), inicio, fim, usuario_id, faixa)
except api.ApiErro as erro:
    sessao.mostrar_erro(erro)
    st.stop()

itens = pagina.get("content", [])
total = pagina.get("totalElements", len(itens))
revisoes = sum(1 for p in itens if p.get("revisaoSolicitada"))

m1, m2, m3 = st.columns(3)
m1.metric("Predições no período", total)
m2.metric("Risco alto", sum(1 for p in itens if p.get("faixaRisco") == "ALTO"))
m3.metric("Pedidos de revisão", revisoes)

if not itens:
    st.info("Nenhuma predição encontrada com esses filtros.")
    st.stop()

tabela = pd.DataFrame(itens)
tabela = pd.DataFrame({
    "Nº": tabela["id"],
    "Data e hora": pd.to_datetime(tabela["criadoEm"], utc=True).dt.tz_convert("America/Sao_Paulo").dt.strftime("%d/%m/%Y %H:%M"),
    "Usuário": tabela.get("usuarioNome", tabela.get("usuarioId")),
    "Faixa de risco": tabela["faixaRisco"].map(FAIXAS).fillna(tabela["faixaRisco"]),
    "Chance (%)": (tabela["probabilidade"] * 100).round(0).astype(int),
    "Versão do modelo": tabela["versaoModelo"],
    "Revisão solicitada": tabela["revisaoSolicitada"].map({True: "Sim", False: "Não"}),
})
st.dataframe(tabela, hide_index=True, width="stretch")

if total > len(itens):
    st.caption(f"Mostrando as {len(itens)} predições mais recentes de {total}. Use os filtros para refinar.")
