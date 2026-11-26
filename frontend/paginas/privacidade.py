import streamlit as st

st.title("Privacidade")
st.caption("Conteúdo provisório, a ser validado pela frente de Governança de Dados (docs/governanca.md).")

st.subheader("Quais dados o sistema coleta")
st.markdown(
    "- **Sobre a gestação:** os dez itens do formulário (faixa etária, situação conjugal, escolaridade, tipo de "
    "gravidez, histórico obstétrico e início do pré-natal). O sistema **não pede** nome, CPF, endereço, telefone "
    "nem data de nascimento.\n"
    "- **Sobre quem usa o sistema:** nome, e-mail e perfil da conta, além do registro de acessos (quem fez cada "
    "ação e quando).\n"
    "- **Resultado de cada predição:** faixa de risco, pontuação do modelo, principais fatores, versão do modelo, data e hora."
)

st.subheader("Para que os dados são usados")
st.write(
    "Somente para estimar o risco de baixo peso ao nascer e apoiar a priorização do acompanhamento pré-natal, "
    "além de permitir a auditoria do uso do sistema."
)

st.subheader("Por quanto tempo são guardados")
st.write("Prazo de retenção a definir pela Governança de Dados. Até lá, os registros ficam guardados para auditoria e não são apagados automaticamente.")

st.subheader("Quem acessa")
st.markdown(
    "- **Operador:** faz predições e vê os próprios resultados.\n"
    "- **Gestor/auditor:** consulta o histórico geral de predições, os pedidos de revisão e o registro de acessos.\n"
    "- O acesso é controlado pelo servidor conforme o perfil. As senhas nunca são guardadas em texto: o sistema "
    "guarda só um hash (BCrypt), que não permite recuperar a senha original."
)

st.subheader("Revisão de decisão automatizada")
st.write(
    "Conforme o art. 20 da LGPD, é possível pedir a revisão humana de qualquer resultado pelo botão "
    "**Solicitar revisão humana**, na tela de resultado."
)
