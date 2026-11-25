import streamlit as st

st.title("Sobre o modelo")
st.caption("Versão do model card voltada ao usuário. Conteúdo provisório: métricas e limiares serão publicados após a avaliação final do modelo (docs/model-card.md).")

st.subheader("Para que serve")
st.write(
    "Estimar o risco de o bebê nascer com **baixo peso (menos de 2.500 g)**, para ajudar a equipe de saúde "
    "da família a priorizar o acompanhamento pré-natal. É um apoio à decisão: não diagnostica nem substitui "
    "a avaliação profissional."
)

st.subheader("Com quais dados foi treinado")
st.markdown(
    "- **Fonte:** SINASC (Sistema de Informações sobre Nascidos Vivos), dados abertos da Prefeitura de São Paulo.\n"
    "- **Período:** nascimentos de 2024, cerca de 120 mil registros.\n"
    "- **Informações usadas:** faixa etária, situação conjugal e escolaridade da gestante; tipo de gravidez; "
    "gestações, partos, filhos vivos e perdas anteriores; mês de início do pré-natal.\n"
    "- **Informações não usadas:** dados que só existem depois do parto (peso, Apgar, tipo de parto, semanas de "
    "gestação no nascimento) e dados pessoais desnecessários, como raça/cor e ocupação."
)

st.subheader("Como avaliar o resultado")
st.write(
    "O sistema prioriza **não deixar passar** gestantes de risco, mesmo que isso gere alguns alertas a mais. "
    "As métricas de desempenho no conjunto de teste serão apresentadas aqui na versão final."
)

st.subheader("Limitações")
st.markdown(
    "- Os dados vêm de declarações de nascimento, preenchidas depois do parto; nem tudo estava disponível em cada consulta.\n"
    "- Gravidez de gêmeos ou mais pesa muito no resultado, porque é um fator forte de baixo peso.\n"
    "- O modelo aprende associações, não causas. Um fator listado não quer dizer que ele provoca o baixo peso.\n"
    "- Foi treinado com dados do município de São Paulo e pode não valer para outras regiões.\n"
    "- Não deve ser usado para negar atendimento nem para decidir condutas sem a avaliação da equipe."
)
