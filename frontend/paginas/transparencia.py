import streamlit as st

# Números do modelo 1.0.0: ml/modelagem.md e ml/artifacts/metadata.json.

st.title("Sobre o modelo")
st.caption(
    "Versão do model card voltada ao usuário. Modelo 1.0.0 (Random Forest). "
    "A versão técnica completa fica em docs/model-card.md e ml/modelagem.md."
)

st.subheader("Para que serve")
st.write(
    "Estimar o risco de o bebê nascer com **baixo peso (menos de 2.500 g)**, para ajudar a equipe de saúde "
    "da família a priorizar o acompanhamento pré-natal. É um apoio à decisão: não diagnostica nem substitui "
    "a avaliação profissional."
)

st.subheader("Com quais dados foi treinado")
st.markdown(
    "- **Fonte:** SINASC (Sistema de Informações sobre Nascidos Vivos), dados abertos da Prefeitura de São Paulo.\n"
    "- **Período:** nascimentos de 2024. Depois de retirar duplicatas e um registro sem peso, ficaram 120.780 "
    "registros: 96.624 para treinar o modelo e 24.156 guardados só para o teste final.\n"
    "- **Informações usadas:** faixa etária, situação conjugal e escolaridade da gestante; tipo de gravidez; "
    "gestações, partos, filhos vivos e perdas anteriores; mês de início do pré-natal.\n"
    "- **Informações não usadas:** dados que só existem depois do parto (peso, Apgar, tipo de parto, semanas de "
    "gestação no nascimento) e dados pessoais desnecessários, como raça/cor e ocupação."
)

st.subheader("Como ler o resultado")
st.markdown(
    "- O modelo dá uma **pontuação de 0 a 1**. Ela serve para ordenar as gestantes por risco; "
    "**não é a chance** de o bebê nascer com baixo peso.\n"
    "- A pontuação é convertida em três faixas, definidas pela posição da gestante em relação às demais "
    "nos dados de treino:\n"
    "    - **Risco baixo:** metade das gestantes com as menores pontuações. Cerca de 7 em cada 100 tiveram bebê "
    "com baixo peso.\n"
    "    - **Risco médio:** as 35% seguintes. Cerca de 10 em cada 100.\n"
    "    - **Risco alto:** as 15% com as maiores pontuações. Cerca de 23 em cada 100, mais que o dobro da média "
    "geral (10 em cada 100).\n"
    "- A faixa de risco alto reúne cerca de **um terço** de todos os casos de baixo peso. Ou seja, muitos casos "
    "aparecem nas faixas baixa e média: o resultado ajuda a priorizar, mas não dispensa o acompanhamento de rotina."
)

st.subheader("Qual o desempenho")
st.markdown(
    "- O modelo foi ajustado para **não deixar passar** gestantes de risco, mesmo gerando mais alertas. "
    "No teste final, ele apontou 74% dos casos de baixo peso, mas a maioria das gestantes apontadas "
    "teve bebê com peso normal.\n"
    "- Comparando uma gestante cujo bebê nasceu com baixo peso com outra cujo bebê nasceu com peso normal, "
    "o modelo dá a pontuação maior à primeira em cerca de **64%** das vezes (um sorteio acertaria 50%). "
    "É um desempenho modesto, esperado porque o formulário não tem informações clínicas como pressão, "
    "peso da gestante ou exames."
)

st.subheader("Limitações")
st.markdown(
    "- Os dados vêm de declarações de nascimento, preenchidas depois do parto; nem tudo estava disponível em cada consulta.\n"
    "- Gravidez de gêmeos ou mais pesa muito no resultado, porque é um fator forte de baixo peso.\n"
    "- Quando o pré-natal ainda não começou, a pontuação sobe. Nos dados históricos, a falta de informação "
    "também aparece junto com mais casos de baixo peso, mas isso não quer dizer que a falta cause risco.\n"
    "- O modelo aprende associações, não causas. Um fator listado não quer dizer que ele provoca o baixo peso.\n"
    "- Foi treinado com dados do município de São Paulo em 2024, não tem validação externa e pode não valer "
    "para outras regiões ou anos.\n"
    "- As faixas não foram validadas clinicamente.\n"
    "- Não deve ser usado para negar atendimento nem para decidir condutas sem a avaliação da equipe."
)
