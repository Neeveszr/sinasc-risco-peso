import sys
import json
from pathlib import Path

import joblib
import numpy as np
import pandas as pd


# Configurar os caminhos do projeto
RAIZ_ML = Path(__file__).resolve().parents[1]

sys.path.insert(0, str(RAIZ_ML / "src"))

PASTA_ARTIFACTS = RAIZ_ML / "artifacts"


# Carregar os artefatos
modelo = joblib.load(
    PASTA_ARTIFACTS / "modelo_sinasc.joblib"
)

with open(
    PASTA_ARTIFACTS / "metadata.json",
    encoding="utf-8"
) as arquivo:
    metadados = json.load(arquivo)

with open(
    PASTA_ARTIFACTS / "input_schema.json",
    encoding="utf-8"
) as arquivo:
    contrato = json.load(arquivo)


# Recuperar as configurações dos metadados
configuracao = metadados["classificacao"]

threshold = configuracao["threshold"]

faixas = configuracao["faixas"]


# Classificar a faixa experimental
def classificar_faixa(score):

    if not np.isfinite(score) or not 0 <= score <= 1:
        raise ValueError("Score invalido.")

    if score < faixas["MEDIO"]["min"]:
        return "BAIXO"

    if score < faixas["ALTO"]["min"]:
        return "MEDIO"

    return "ALTO"


# Validar as configurações
assert threshold == 0.43

assert configuracao["score_calibrado"] is False

assert set(faixas) == {"BAIXO", "MEDIO", "ALTO"}

assert (
    faixas["BAIXO"]["max"]
    == faixas["MEDIO"]["min"]
)

assert (
    faixas["MEDIO"]["max"]
    == faixas["ALTO"]["min"]
)


# Testar os limites das faixas
limite_medio = faixas["MEDIO"]["min"]

limite_alto = faixas["ALTO"]["min"]

assert 0 < limite_medio < limite_alto < 1

assert classificar_faixa(0.0) == "BAIXO"

assert classificar_faixa(
    np.nextafter(limite_medio, -np.inf)
) == "BAIXO"

assert classificar_faixa(limite_medio) == "MEDIO"

assert classificar_faixa(
    np.nextafter(limite_alto, -np.inf)
) == "MEDIO"

assert classificar_faixa(limite_alto) == "ALTO"

assert classificar_faixa(1.0) == "ALTO"


# Registro fictício para teste de inferência
dados = {
    "FxEtPartur": "25-29",
    "ESTCIVMAE": 2,
    "ESCMAE2010": 4,
    "GRAVIDEZ": 1,
    "QTDGESTANT": 2,
    "QTDPARTNOR": 1,
    "QTDPARTCES": 1,
    "QTDFILVIVO": 2,
    "QTDFILMORT": 0,
    "MESPRENAT": 3
}


# Validar os atributos de entrada
assert set(dados) == set(contrato["atributos"])

assert "Ign" not in contrato["atributos"][
    "FxEtPartur"
]["valores_aceitos"]


# Realizar a inferência
entrada = pd.DataFrame([dados])

score = float(
    modelo.predict_proba(entrada)[0, 1]
)

assert np.isfinite(score)
assert 0 <= score <= 1

classe = int(score >= threshold)

faixa = classificar_faixa(score)


# Resultado experimental
resultado = {
   "versao_modelo": metadados["versao"],
    "score": round(score, 6),
    "score_calibrado": False,
    "classe_prevista": classe,
    "faixa_experimental": faixa,
    "threshold": threshold,
    "finalidade": "Exclusivamente academica"
}

print(json.dumps(
    resultado,
    indent=4,
    ensure_ascii=False
))

print("\nValidacao dos metadados: OK")
print("Validacao das faixas: OK")
print("Validacao dos atributos: OK")
print("Teste de inferencia: OK")