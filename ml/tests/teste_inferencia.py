import sys
import json
import joblib
import pandas as pd
import numpy as np
from pathlib import Path

# Localizar a pasta ml
pasta_ml = Path(__file__).resolve().parents[1]

# Disponibilizar os módulos personalizados
sys.path.insert(0, str(pasta_ml / "src"))

# Localizar os artefatos
pasta_artifacts = pasta_ml / "artifacts"

# Carregar o modelo treinado
modelo = joblib.load(
    pasta_artifacts / "modelo_sinasc.joblib"
)

# Carregar os metadados
with open(
    pasta_artifacts / "metadata.json",
    encoding="utf-8"
) as arquivo:
    metadata = json.load(arquivo)

# Carregar o contrato de entrada
with open(
    pasta_artifacts / "input_schema.json",
    encoding="utf-8"
) as arquivo:
    schema = json.load(arquivo)

# Registro exclusivamente fictício
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

# Verificar os campos de entrada
atributos = schema["campos_obrigatorios"]

assert set(dados.keys()) == set(atributos)

# Converter para DataFrame
entrada = pd.DataFrame(
    [dados],
    columns=atributos
)

# Identificar a posição da classe positiva
indice_positivo = list(modelo.classes_).index(1)

# Realizar a inferência
score = float(
    modelo.predict_proba(entrada)[0, indice_positivo]
)

# Validar o score
assert np.isfinite(score)
assert 0 <= score <= 1

# Recuperar os limites dos metadados
threshold = metadata["classificacao"]["threshold"]

limite_moderado = (
    metadata["classificacao"]["faixas"]["moderado"]["min"]
)

# Aplicar a classificação experimental
classe = int(score >= threshold)

if score < limite_moderado:
    faixa = "Baixo"
elif score < threshold:
    faixa = "Moderado"
else:
    faixa = "Alto"

# Exibir os resultados
resultado = {
    "versao_modelo": metadata["versao"],
    "score_experimental": round(score, 6),
    "classe_prevista": classe,
    "faixa_experimental": faixa,
    "threshold": threshold,
    "finalidade": "Exclusivamente academica"
}

print("Modelo carregado com sucesso!")
print("Contrato de entrada: OK")
print("\nResultado da inferencia:")

print(json.dumps(
    resultado,
    indent=4,
    ensure_ascii=False
))

print("\nTeste independente: OK")