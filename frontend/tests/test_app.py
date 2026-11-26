"""Testes das telas em modo demonstração (sem API).

Rodar a partir da pasta frontend/:  python -m pytest
"""

import os
import sys
from pathlib import Path

os.environ["API_MOCK"] = "1"
PASTA = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(PASTA))

from streamlit.testing.v1 import AppTest  # noqa: E402

import campos  # noqa: E402

APP = str(PASTA / "app.py")


def entrar(email):
    at = AppTest.from_file(APP, default_timeout=30).run()
    at.text_input[0].input(email)
    at.text_input[1].input("qualquer-senha")
    at.button[0].click().run()
    assert not at.exception
    return at


def botao(at, rotulo):
    return next(b for b in at.button if b.label == rotulo)


def textos(at):
    return " ".join(e.value for e in [*at.markdown, *at.title, *at.header, *at.subheader, *at.caption] if isinstance(e.value, str))


def test_login_vazio_mostra_erro():
    at = AppTest.from_file(APP).run()
    at.button[0].click().run()
    assert any("Informe e-mail e senha" in e.value for e in at.error)


def test_operador_ve_formulario_e_nao_ve_auditoria():
    at = entrar("operador@ubs.sp.gov.br")
    assert at.title[0].value == "Nova predição"
    assert "Auditoria" not in textos(at)


def test_predicao_gemelar_gera_risco_alto_e_permite_revisao():
    at = entrar("operador@ubs.sp.gov.br")
    at.selectbox(key="tipoGravidez").select(2)
    botao(at, "Calcular risco").click().run()
    assert not at.exception
    assert at.title[0].value == "Resultado"
    assert "Risco alto" in at.header[0].value
    assert any("apoio à decisão" in w.value for w in at.warning)

    botao(at, "Solicitar revisão humana").click().run()
    assert any("Revisão humana solicitada" in s.value for s in at.success)


def test_resultado_mostra_pontuacao_e_nunca_chance():
    at = entrar("operador@ubs.sp.gov.br")
    at.selectbox(key="tipoGravidez").select(1)
    botao(at, "Calcular risco").click().run()
    assert not at.exception
    assert at.metric[0].label == "Pontuação do modelo (de 0 a 1)"
    assert "%" not in at.metric[0].value
    assert "chance" not in textos(at).lower().replace("não é a chance", "")


def test_tipo_de_gravidez_e_obrigatorio():
    at = entrar("operador@ubs.sp.gov.br")
    assert at.selectbox(key="tipoGravidez").value is None
    botao(at, "Calcular risco").click().run()
    assert any("Tipo de gravidez: selecione" in e.value for e in at.error)
    assert at.title[0].value == "Nova predição"


def test_partos_maior_que_gestacoes_e_rejeitado():
    at = entrar("operador@ubs.sp.gov.br")
    at.selectbox(key="tipoGravidez").select(1)
    at.number_input(key="partosNormais").set_value(2)
    botao(at, "Calcular risco").click().run()
    assert any("não pode ser maior" in e.value for e in at.error)
    assert at.title[0].value == "Nova predição"


def test_gestor_ve_auditoria_com_historico():
    at = entrar("gestor@ubs.sp.gov.br")
    assert at.title[0].value == "Auditoria das predições"
    assert len(at.dataframe) == 1
    assert at.metric[0].value != "0"


def test_gestor_filtra_auditoria_por_usuario_pelo_nome():
    at = entrar("gestor@ubs.sp.gov.br")
    filtro = next(s for s in at.selectbox if s.label == "Usuário")
    assert filtro.format_func(None) == "Todos"
    filtro.select(2)  # Operador Demo
    botao(at, "Filtrar").click().run()
    assert not at.exception
    usuarios = set(at.dataframe[0].value["Usuário"])
    assert usuarios == {"Operador Demo"}


def test_privacidade_fala_em_hash_e_nao_em_criptografia():
    at = entrar("operador@ubs.sp.gov.br")
    at.switch_page("paginas/privacidade.py").run()
    texto = textos(at)
    assert "hash (BCrypt)" in texto
    assert "criptografada" not in texto


def test_validacao_regras_basicas():
    base = {c["api"]: 0 for c in campos.CONTAGENS} | {"mesInicioPrenatal": 2, "tipoGravidez": 1}
    assert campos.validar(base) == []
    assert campos.validar(base | {"mesInicioPrenatal": None}) == []
    assert campos.validar(base | {"faixaEtaria": None, "estadoCivil": None, "escolaridade": None}) == []
    assert campos.validar(base | {"tipoGravidez": None})
    assert campos.validar(base | {"mesInicioPrenatal": 10})
    assert campos.validar(base | {"partosCesareos": 16, "gestacoesAnteriores": 16})


def test_faixas_do_mock_seguem_os_limites_do_modelo():
    import mock

    assert mock._faixa(0.44) == "BAIXO"  # acima do limiar 0,43 da classe, mas a faixa é BAIXO
    assert mock._faixa(mock.LIMITE_MEDIO) == "MEDIO"
    assert mock._faixa(mock.LIMITE_ALTO) == "ALTO"
