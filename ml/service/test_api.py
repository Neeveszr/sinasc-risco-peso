"""Testes do contrato. Rodar dentro de ml/service: pytest"""
import importlib
import os

import pytest
from fastapi.testclient import TestClient

VALIDA = {
    "FxEtPartur": "25-29", "ESTCIVMAE": 5, "ESCMAE2010": 3, "GRAVIDEZ": 1,
    "QTDGESTANT": 1, "QTDPARTNOR": 1, "QTDPARTCES": 0,
    "QTDFILVIVO": 1, "QTDFILMORT": 0, "MESPRENAT": 2,
}
# Registro fictício do teste do Marcus (ml/tests/teste_inferencia.py)
REGISTRO_MARCUS = {
    "FxEtPartur": "25-29", "ESTCIVMAE": 2, "ESCMAE2010": 4, "GRAVIDEZ": 1,
    "QTDGESTANT": 2, "QTDPARTNOR": 1, "QTDPARTCES": 1,
    "QTDFILVIVO": 2, "QTDFILMORT": 0, "MESPRENAT": 3,
}
ATRIBUTOS = set(VALIDA)


def carregar_app(mock: bool):
    os.environ["MODELO_MOCK"] = "1" if mock else "0"
    import main
    importlib.reload(main)
    return TestClient(main.app)


@pytest.fixture(scope="module")
def mock():
    return carregar_app(True)


@pytest.fixture(scope="module")
def real():
    return carregar_app(False)


def com(**mudancas):
    return {**VALIDA, **mudancas}


# ---------- mock ----------
def test_mock_basico(mock):
    r = mock.post("/predict", json=VALIDA)
    assert r.status_code == 200
    corpo = r.json()
    assert corpo["faixa_risco"] == "BAIXO"
    assert corpo["versao_modelo"] == "0.0.0-mock"
    assert "probabilidade" not in corpo and "classe_prevista" not in corpo


def test_mock_prioridade(mock):
    r = mock.post("/predict", json=com(MESPRENAT=None, GRAVIDEZ=2))
    assert r.json()["faixa_risco"] == "ALTO"          # primeira regra vence
    r = mock.post("/predict", json=com(GRAVIDEZ=3))
    assert r.json()["faixa_risco"] == "MEDIO"


def test_versao_e_health(mock):
    assert mock.get("/health").json() == {"status": "ok"}
    assert mock.get("/model/version").json()["versao_modelo"] == "0.0.0-mock"


# ---------- validação (vale para mock e real) ----------
@pytest.mark.parametrize("corpo", [
    com(PESO=3000),                       # campo extra
    com(FxEtPartur="25–29"),              # travessão
    com(FxEtPartur="Ign"),                # texto Ign
    com(ESTCIVMAE=7),                     # fora da lista
    com(GRAVIDEZ=0),
    com(MESPRENAT=10),
    com(MESPRENAT=99),
    com(QTDGESTANT=16),                   # acima do máximo
    com(QTDGESTANT=-1),
    com(QTDGESTANT="1"),                  # texto no lugar de número
    com(GRAVIDEZ=True),                   # booleano no lugar de número
    com(QTDGESTANT=1, QTDPARTNOR=1, QTDPARTCES=1),   # partos > gestações
    {k: v for k, v in VALIDA.items() if k != "MESPRENAT"},  # campo ausente
])
def test_entradas_invalidas_422(mock, corpo):
    assert mock.post("/predict", json=corpo).status_code == 422


def test_nulos_e_codigo_9_aceitos(mock):
    corpo = com(FxEtPartur=None, ESTCIVMAE=9, ESCMAE2010=None, GRAVIDEZ=9, MESPRENAT=None)
    assert mock.post("/predict", json=corpo).status_code == 200


def test_mesprenat_9_e_o_nono_mes(mock):
    assert mock.post("/predict", json=com(MESPRENAT=9)).status_code == 200


# ---------- modelo real ----------
def test_real_registro_do_marcus(real):
    r = real.post("/predict", json=REGISTRO_MARCUS)
    assert r.status_code == 200
    corpo = r.json()
    assert corpo["score"] == pytest.approx(0.339087, abs=1e-5)  # mesmo valor do teste dele
    assert corpo["faixa_risco"] == "BAIXO"
    assert corpo["versao_modelo"] == "1.0.0"


def test_real_fatores_por_atributo_original(real):
    fatores = real.post("/predict", json=REGISTRO_MARCUS).json()["fatores"]
    assert len(fatores) == 3
    for f in fatores:
        assert f["atributo"] in ATRIBUTOS          # nunca 'FxEtPartur_25-29'
        assert f["direcao"] in ("AUMENTA", "REDUZ")
        assert f["impacto"] >= 0
    impactos = [f["impacto"] for f in fatores]
    assert impactos == sorted(impactos, reverse=True)


def test_real_gravidez_ausente_sobe_o_score(real):
    base = real.post("/predict", json=REGISTRO_MARCUS).json()["score"]
    ausente = real.post("/predict", json={**REGISTRO_MARCUS, "GRAVIDEZ": None}).json()
    assert ausente["score"] > base   # limitação já registrada no metadata.json
    assert ausente["faixa_risco"] == "ALTO"


def test_real_codigo_9_igual_a_null(real):
    a = real.post("/predict", json={**REGISTRO_MARCUS, "GRAVIDEZ": 9}).json()
    b = real.post("/predict", json={**REGISTRO_MARCUS, "GRAVIDEZ": None}).json()
    assert a["score"] == b["score"]
