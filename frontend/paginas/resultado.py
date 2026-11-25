import pandas as pd
import streamlit as st

import api
import campos
import sessao

FAIXAS = {
    "BAIXO": ("Risco baixo", "green", "Manter o acompanhamento pré-natal de rotina."),
    "MEDIO": ("Risco médio", "orange", "Reforçar o acompanhamento e observar os fatores abaixo."),
    "ALTO": ("Risco alto", "red", "Priorizar esta gestante no acompanhamento da equipe."),
}

st.title("Resultado")

resultado = st.session_state.get("ultimo_resultado")
if not resultado:
    st.info("Nenhuma predição feita nesta sessão.")
    st.page_link("paginas/predicao.py", label="Fazer uma predição", icon=":material/edit_note:")
    st.stop()

titulo, cor, orientacao = FAIXAS.get(resultado["faixaRisco"], (resultado["faixaRisco"], "gray", ""))
st.header(f":{cor}[{titulo}]")
st.write(orientacao)
st.metric("Chance estimada de baixo peso ao nascer", f"{resultado['probabilidade'] * 100:.0f}%")

st.subheader("O que mais pesou nesta avaliação")
fatores = resultado.get("fatores") or []
if not fatores:
    st.caption("O modelo não informou fatores para esta predição.")
for fator in fatores:
    aumenta = fator.get("efeito") == "AUMENTA"
    seta = ":material/arrow_upward:" if aumenta else ":material/arrow_downward:"
    efeito = "aumenta o risco" if aumenta else "reduz o risco"
    nome = fator.get("descricao") or campos.ROTULOS.get(fator.get("atributo"), fator.get("atributo"))
    st.markdown(f"{seta} **{nome}**: {efeito}")

criado = pd.Timestamp(resultado["criadoEm"]).tz_convert("America/Sao_Paulo")
st.caption(f"Predição nº {resultado['id']} · {criado:%d/%m/%Y %H:%M} · versão do modelo: {resultado['versaoModelo']}")

st.warning(
    "**Este resultado é um apoio à decisão.** Ele estima um risco a partir de dados de nascimentos "
    "anteriores e não substitui a avaliação da equipe de saúde. Nenhuma conduta deve ser tomada só com base nele.",
    icon=":material/medical_information:",
)

st.subheader("Revisão humana")
if resultado.get("revisaoSolicitada"):
    st.success("Revisão humana solicitada. Um profissional responsável vai analisar esta predição.")
else:
    st.write("Se o resultado parecer incorreto ou precisar de análise, peça a revisão de um profissional.")
    with st.form("revisao"):
        motivo = st.text_area(
            "Motivo (opcional)", max_chars=500,
            help="Descreva o motivo sem incluir nome ou dados que identifiquem a gestante.",
        )
        solicitar = st.form_submit_button("Solicitar revisão humana")
    if solicitar:
        try:
            api.solicitar_revisao(sessao.token(), resultado["id"], motivo.strip())
            resultado["revisaoSolicitada"] = True
            st.rerun()
        except api.ApiErro as erro:
            sessao.mostrar_erro(erro)
