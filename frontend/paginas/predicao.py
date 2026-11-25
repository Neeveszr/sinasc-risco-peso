import streamlit as st

import api
import campos
import sessao

st.title("Nova predição")
st.write("Preencha com as informações do pré-natal da gestante.")
st.info(
    "Não digite nome, CPF, endereço, telefone ou qualquer dado que identifique a gestante. "
    "O sistema não precisa dessas informações.",
    icon=":material/privacy_tip:",
)

dados = {}
with st.form("predicao"):
    st.subheader("Perfil da gestante")
    colunas = st.columns(2)
    for i, campo in enumerate(campos.CATEGORICOS):
        opcoes = [None, *campo["opcoes"]]
        with colunas[i % 2]:
            dados[campo["api"]] = st.selectbox(
                campo["rotulo"],
                opcoes,
                format_func=lambda valor, c=campo: "Não informado" if valor is None else c["opcoes"][valor],
                key=campo["api"],
            )

    st.subheader("Histórico obstétrico")
    colunas = st.columns(3)
    for i, campo in enumerate(campos.CONTAGENS):
        with colunas[i % 3]:
            dados[campo["api"]] = st.number_input(
                campo["rotulo"], min_value=0, max_value=campos.MAX_CONTAGEM, value=0, step=1, key=campo["api"]
            )

    st.subheader("Pré-natal")
    sem_prenatal = st.checkbox("A gestante ainda não iniciou o pré-natal", key="sem_prenatal")
    mes = st.number_input(
        campos.MES_PRENATAL["rotulo"], min_value=1, max_value=9, value=1, step=1, key="mesInicioPrenatal",
        help="Ignorado quando a opção acima está marcada.",
    )
    dados[campos.MES_PRENATAL["api"]] = None if sem_prenatal else mes

    enviar = st.form_submit_button("Calcular risco", type="primary")

if enviar:
    erros = campos.validar(dados)
    if erros:
        for erro in erros:
            st.error(erro)
    else:
        try:
            with st.spinner("Calculando..."):
                resultado = api.criar_predicao(sessao.token(), dados)
            st.session_state["ultimo_resultado"] = resultado
            st.switch_page("paginas/resultado.py")
        except api.ApiErro as erro:
            sessao.mostrar_erro(erro)
